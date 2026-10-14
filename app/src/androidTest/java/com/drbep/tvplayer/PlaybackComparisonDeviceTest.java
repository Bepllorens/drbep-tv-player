package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DecoderCounters;
import android.os.SystemClock;
import android.util.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class PlaybackComparisonDeviceTest {
 static Object field(Object o,String n) {try{java.lang.reflect.Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
 static void call(Object o,String n) {try{java.lang.reflect.Method m=o.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(o);}catch(Exception e){throw new AssertionError(e);}}
 static ExoPlayer player(MainActivity a){return (ExoPlayer)field(field(a,"playerController"),"player");}
 static long frames(MainActivity a){ExoPlayer p=player(a);if(p==null)return 0;DecoderCounters c=p.getVideoDecoderCounters();if(c==null)return 0;c.ensureUpdated();return c.renderedOutputBufferCount;}
 static void awaitVideo(ActivityScenario<MainActivity> s,String label)throws Exception {
  long start=SystemClock.elapsedRealtime(); final boolean[] ok={false}; final long[] sample={-1,-1};
  while(SystemClock.elapsedRealtime()-start<45000){
   s.onActivity(a->{ExoPlayer p=player(a);long f=frames(a),pos=p==null?0:p.getCurrentPosition();ok[0]=p!=null&&p.isPlaying()&&f>sample[0]+5&&pos>sample[1]+100&&sample[0]>=0;sample[0]=f;sample[1]=pos;});
   if(ok[0])break;Thread.sleep(500);
  }
  Log.i("PlaybackCompare",label+" videoMs="+(SystemClock.elapsedRealtime()-start)+" rendered="+sample[0]+" success="+ok[0]);
  assertTrue(label+" must render advancing video",ok[0]);
 }
 @Test public void zappingAndHomePreview()throws Exception {
  try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(12000);final java.util.List<ChannelItem> candidates=new java.util.ArrayList<>();
   s.onActivity(a->{ComposeDialogHost.dismissAll(a);for(ChannelItem c:(java.util.List<ChannelItem>)field(a,"allChannels")){
    String name=FanartRepository.channelKey(c.name);if(c.platformName.toLowerCase(java.util.Locale.ROOT).contains("ism")&&(name.equals("la1")||name.contains("vamos")||name.contains("calle13")))candidates.add(c);
   }});
   assertTrue("Need three reference channels",candidates.size()>=3);
   for(int i=0;i<6;i++) {ChannelItem c=candidates.get(i%3);long start=SystemClock.elapsedRealtime();
    s.onActivity(a->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("tuneChannelById",String.class);m.setAccessible(true);m.invoke(a,c.id);}catch(Exception e){throw new AssertionError(e);}});
    awaitVideo(s,"zap-"+i+" "+c.name);Log.i("PlaybackCompare","zapTotalMs="+(SystemClock.elapsedRealtime()-start));Thread.sleep(1500);
   }
   for(int i=0;i<3;i++) {
    long start=SystemClock.elapsedRealtime();s.onActivity(a->{Object state=field(a,"discoveryHomeState");try{java.lang.reflect.Field f=state.getClass().getDeclaredField("section");f.setAccessible(true);f.set(state,"Inicio");}catch(Exception e){throw new AssertionError(e);}call(a,"loadStartupHubStateAndShow");});
    final boolean[] preview={false};while(SystemClock.elapsedRealtime()-start<20000){s.onActivity(a->{preview[0]=field(a,"startupLivePreviewPlayerView")!=null;});if(preview[0])break;Thread.sleep(200);}
    assertTrue("Home preview attached",preview[0]);awaitVideo(s,"preview-"+i);Log.i("PlaybackCompare","previewTotalMs="+(SystemClock.elapsedRealtime()-start));
    Thread.sleep(2000);
    final int[] rect=new int[4];s.onActivity(a->{android.view.View v=(android.view.View)field(a,"startupLivePreviewPlayerView");int[] xy=new int[2];v.getLocationOnScreen(xy);rect[0]=xy[0];rect[1]=xy[1];rect[2]=v.getWidth();rect[3]=v.getHeight();});
    android.graphics.Bitmap shot=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    int lit=0,total=0;for(int y=rect[1]+rect[3]/4;y<rect[1]+3*rect[3]/4;y+=8)for(int x=rect[0]+rect[2]/4;x<rect[0]+3*rect[2]/4;x+=8){if(x>=0&&y>=0&&x<shot.getWidth()&&y<shot.getHeight()){int c=shot.getPixel(x,y);if(android.graphics.Color.red(c)>25||android.graphics.Color.green(c)>25||android.graphics.Color.blue(c)>25)lit++;total++;}}
    java.io.File out=new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"comparison-preview-"+i+".png");try(java.io.FileOutputStream f=new java.io.FileOutputStream(out)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,f);}shot.recycle();
    Log.i("PlaybackCompare","previewPixels round="+i+" lit="+lit+" total="+total);assertTrue("Preview must contain visible image",total>0&&lit>total/100);
    s.onActivity(a->{ComposeDialogHost.dismissAll(a);});Thread.sleep(300);s.onActivity(a->assertNull(field(a,"startupLivePreviewPlayerView")));awaitVideo(s,"fullscreen-"+i);
   }
  }
 }
}
