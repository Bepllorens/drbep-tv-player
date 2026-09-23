package com.drbep.tvplayer;

import android.util.Log;
import androidx.media3.exoplayer.hls.DefaultHlsExtractorFactory;
import androidx.media3.exoplayer.hls.HlsExtractorFactory;
import androidx.media3.exoplayer.hls.HlsMediaChunkExtractor;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.extractor.TrackOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Isolated, opt-in normalization of empty public IDs. Never changes ciphertext. */
final class ObservingHlsFactory {
  static Object invoke(Object target, Method method, Object[] args) throws Throwable {
    try {return method.invoke(target,args);}
    catch(InvocationTargetException exception){throw exception.getCause();}
  }
  static HlsExtractorFactory create(boolean normalize,PlaylistPublicIds ids) {
    HlsExtractorFactory original=new DefaultHlsExtractorFactory();
    return (HlsExtractorFactory)Proxy.newProxyInstance(HlsExtractorFactory.class.getClassLoader(),
      new Class<?>[]{HlsExtractorFactory.class},(proxy,method,args)->{
        Object result=invoke(original,method,args);
        if(result==original)return proxy;
        return method.getName().equals("createExtractor")?observe((HlsMediaChunkExtractor)result,normalize,ids,args[0].toString()):result;
      });
  }
  static HlsMediaChunkExtractor observe(HlsMediaChunkExtractor original,boolean normalize,PlaylistPublicIds ids,String uri) {
    return (HlsMediaChunkExtractor)Proxy.newProxyInstance(HlsMediaChunkExtractor.class.getClassLoader(),
      new Class<?>[]{HlsMediaChunkExtractor.class},(proxy,method,args)->{
        if(method.getName().equals("init")) {
          ExtractorOutput output=(ExtractorOutput)args[0];
          args=args.clone();
          args[0]=Proxy.newProxyInstance(ExtractorOutput.class.getClassLoader(),new Class<?>[]{ExtractorOutput.class},
            (p,m,a)->{
              Object result=invoke(output,m,a);
              if(!m.getName().equals("track"))return result;
              TrackOutput track=(TrackOutput)result;
              int type=(Integer)a[1];
              boolean[] reported={false};
              byte[][] publicId={null};
              return Proxy.newProxyInstance(TrackOutput.class.getClassLoader(),new Class<?>[]{TrackOutput.class},
                (tp,tm,ta)->{
                  if(tm.getName().equals("format")) {
                    androidx.media3.common.DrmInitData data=((androidx.media3.common.Format)ta[0]).drmInitData;
                    publicId[0]=PublicInitId.single(data);
                    Log.w("AppleNativeProbe","formatHasDrmData="+(data!=null)+" parsedPublicId="+(publicId[0]!=null));
                  }
                  if(tm.getName().equals("sampleMetadata") && ta[4] instanceof TrackOutput.CryptoData) {
                    TrackOutput.CryptoData crypto=(TrackOutput.CryptoData)ta[4];
                    byte[] id=((TrackOutput.CryptoData)ta[4]).encryptionKey;
                    boolean zero=id.length==16;
                    for(byte value:id)zero &= value==0;
                    byte[] manifestId=ids.get(uri);
                    byte[] chosen=publicId[0]==null?manifestId:publicId[0];
                    if(manifestId==null||(chosen!=null&&!java.util.Arrays.equals(chosen,manifestId)))chosen=null;
                    boolean fix=normalize&&zero&&chosen!=null&&!java.util.Arrays.equals(chosen,new byte[16])&&(type==1||type==2);
                    if(!reported[0]) {
                      Log.w("AppleNativeProbe","sampleTrackType="+type+" sampleKeyIdentifierIsZero="+zero+" publicIdAvailable="+(chosen!=null)+" normalized="+fix);
                      reported[0]=true;
                    }
                    if(fix) {
                      ta=ta.clone();
                      ta[4]=new TrackOutput.CryptoData(crypto.cryptoMode,chosen.clone(),crypto.encryptedBlocks,crypto.clearBlocks);
                    }
                  }
                  return invoke(track,tm,ta);
                });
            });
        }
        Object result=invoke(original,method,args);
        return method.getName().equals("recreate")?observe((HlsMediaChunkExtractor)result,normalize,ids,uri):result;
      });
  }
}
