package com.drbep.tvplayer;

import android.net.Uri;
import android.util.Base64;
import androidx.media3.common.*;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.drm.*;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import java.lang.reflect.*;
import java.util.*;

/** Apple-only native CDM compatibility. No license contents are interpreted. */
final class AppleNativePlayback {
    static boolean accepts(MediaItem item) {
        if(item.localConfiguration==null||item.localConfiguration.drmConfiguration==null)return false;
        Uri media=item.localConfiguration.uri;
        MediaItem.DrmConfiguration drm=item.localConfiguration.drmConfiguration;
        Uri license=drm.licenseUri;
        if(!C.WIDEVINE_UUID.equals(drm.scheme)||license==null||!"https".equals(media.getScheme())
                ||!Objects.equals(media.getScheme(),license.getScheme())
                ||!Objects.equals(media.getAuthority(),license.getAuthority()))return false;
        String path=media.getPath(),licensePath=license.getPath();
        if(path==null||!path.matches("/api/vod/private/appletv/play/umc\\.cmc\\.[a-z0-9]{1,64}/master\\.m3u8"))return false;
        String id=path.substring("/api/vod/private/appletv/play/".length(),path.length()-"/master.m3u8".length());
        return ("/api/vod/private/appletv/license/"+id).equals(licensePath);
    }

    static MediaSource.Factory wrap(MediaSource.Factory standard,DataSource.Factory dataSource) {
        return (MediaSource.Factory)Proxy.newProxyInstance(MediaSource.Factory.class.getClassLoader(),
                new Class<?>[]{MediaSource.Factory.class},(proxy,method,args)->{
            if(method.getName().equals("createMediaSource")) {
                MediaItem candidate=(MediaItem)args[0];
                if(candidate.localConfiguration!=null && candidate.localConfiguration.uri.getPath()!=null
                    && candidate.localConfiguration.uri.getPath().contains("/appletv/"))
                    android.util.Log.w("AppleNativePlayback","nativeFactory="+accepts(candidate));
            }
            if(method.getName().equals("createMediaSource")&&accepts((MediaItem)args[0])) {
                MediaItem item=(MediaItem)args[0];
                PlaylistPublicIds ids=new PlaylistPublicIds();
                return new HlsMediaSource.Factory(dataSource).setPlaylistParserFactory(ids)
                    .setExtractorFactory(ObservingHlsFactory.create(true,ids))
                    .setDrmSessionManagerProvider(AppleNativePlayback::manager).createMediaSource(item);
            }
            Object result=ObservingHlsFactory.invoke(standard,method,args);
            return result==standard?proxy:result;
        });
    }

    private static DrmSessionManager manager(MediaItem item) {
        MediaItem.DrmConfiguration config=item.localConfiguration.drmConfiguration;
        String license=config.licenseUri.toString();
        // The bounded native worker may resolve a new audio context on demand.
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10000).setReadTimeoutMs(100000);
        HttpMediaDrmCallback callback=new HttpMediaDrmCallback(license,false,http);
        for(Map.Entry<String,String> header:config.licenseRequestHeaders.entrySet())
            callback.setKeyRequestProperty(header.getKey(),header.getValue());
        return new DefaultDrmSessionManager.Builder().setMultiSession(true)
            .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID,uuid->{
                ExoMediaDrm delegate=FrameworkMediaDrm.DEFAULT_PROVIDER.acquireExoMediaDrm(uuid);
                return (ExoMediaDrm)Proxy.newProxyInstance(ExoMediaDrm.class.getClassLoader(),new Class<?>[]{ExoMediaDrm.class},
                    (proxy,method,args)->{
                        Object result=ObservingHlsFactory.invoke(delegate,method,args);
                        if(!method.getName().equals("getKeyRequest")||!(result instanceof ExoMediaDrm.KeyRequest)||!(args[1] instanceof List))return result;
                        byte[] init=null;
                        for(Object value:(List<?>)args[1]) {
                            DrmInitData.SchemeData data=(DrmInitData.SchemeData)value;
                            if(!data.matches(C.WIDEVINE_UUID)||data.data==null)continue;
                            if(init!=null&&!Arrays.equals(init,data.data))throw new IllegalStateException("Ambiguous Apple track context");
                            init=data.data;
                        }
                        if(init==null||init.length>49152)throw new IllegalStateException("Missing Apple track context");
                        android.util.Log.w("AppleNativePlayback","nativeLicenseRequest contextBytes="+init.length);
                        ExoMediaDrm.KeyRequest request=(ExoMediaDrm.KeyRequest)result;
                        String uri=Uri.parse(license).buildUpon().appendQueryParameter("init_data",Base64.encodeToString(init,Base64.NO_WRAP)).build().toString();
                        return new ExoMediaDrm.KeyRequest(request.getData(),uri,request.getRequestType());
                    });
            }).build(callback);
    }
}
