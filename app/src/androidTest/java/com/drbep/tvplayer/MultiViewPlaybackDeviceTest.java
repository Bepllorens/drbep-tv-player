package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DecoderCounters;
import org.junit.Test;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import java.util.*;import android.view.KeyEvent;
@RunWith(AndroidJUnit4.class)
public class MultiViewPlaybackDeviceTest {
 Object f(Object o,String n){return PlaybackComparisonDeviceTest.field(o,n);}
 void key(ActivityScenario<MainActivity>s,int k){s.onActivity(a->CatchupFocusDeviceTest.key(a,k));}
 void video(ActivityScenario<MainActivity>s)throws Exception{
  long end=android.os.SystemClock.elapsedRealtime()+45000;final long[] previous={-1,-1};final boolean[] good={false};
  while(android.os.SystemClock.elapsedRealtime()<end){s.onActivity(a->{List<PlayerController> list=(List<PlayerController>)f(a,"multiPlayerControllers");good[0]=list.size()>=2;for(int i=0;i<2&&i<list.size();i++){ExoPlayer p=(ExoPlayer)f(list.get(i),"player");DecoderCounters c=p==null?null:p.getVideoDecoderCounters();if(c!=null)c.ensureUpdated();long frames=c==null?0:c.renderedOutputBufferCount;good[0]&=p!=null&&p.isPlaying()&&previous[i]>=0&&frames>previous[i]+3;previous[i]=frames;}});if(good[0])return;Thread.sleep(600);}assertTrue("Both windows must render advancing video",good[0]);
 }
 void sharedImage(ActivityScenario<MainActivity>s)throws Exception{
  Thread.sleep(2500);
  android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(640,360,android.graphics.Bitmap.Config.ARGB_8888);
  java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(1);final int[] result={-1};
  s.onActivity(a->{SharedMultiViewCompositor c=(SharedMultiViewCompositor)f(a,"multiCompositor");assertNotNull("Shared compositor must remain active",c);assertTrue(c.diagnostics().optLong("draws")>5);assertTrue(c.diagnostics().optLong("inputFrames")>10);assertFalse(c.diagnostics().optBoolean("failed"));
   android.view.PixelCopy.request(c,b,status->{result[0]=status;latch.countDown();},new android.os.Handler(android.os.Looper.getMainLooper()));android.util.Log.i("SharedMultiTest",c.diagnostics().toString());});
  assertTrue(latch.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(android.view.PixelCopy.SUCCESS,result[0]);
  for(int tile=0;tile<2;tile++){int lit=0;for(int y=120;y<240;y+=8)for(int x=tile*320+40;x<tile*320+280;x+=8){int color=b.getPixel(x,y);if(android.graphics.Color.red(color)>25||android.graphics.Color.green(color)>25||android.graphics.Color.blue(color)>25)lit++;}assertTrue("Both composed tiles must show pixels",lit>5);}b.recycle();
 }
 void sample(ActivityScenario<MainActivity>s,String mode)throws Exception{
  long cpu=android.os.Process.getElapsedCpuTime(),start=android.os.SystemClock.elapsedRealtime();
  Thread.sleep(12000);long elapsed=android.os.SystemClock.elapsedRealtime()-start,used=android.os.Process.getElapsedCpuTime()-cpu;
  s.onActivity(a->{android.os.Debug.MemoryInfo info=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(info);android.util.Log.i("SharedMultiTest","sample mode="+mode+" elapsedMs="+elapsed+" cpuMs="+used+" pssKb="+info.getTotalPss());});
 }
 @Test public void remoteKeepsPlayersAcrossExpandedView()throws Exception{
  try(ActivityScenario<MainActivity>s=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(12000);final List<Object> players=new ArrayList<>();
   s.onActivity(a->{ComposeDialogHost.dismissAll(a);List<ChannelItem> selected=new ArrayList<>();for(ChannelItem c:(List<ChannelItem>)f(a,"allChannels")){String n=FanartRepository.channelKey(c.name);if(c.platformName.toLowerCase(Locale.ROOT).contains("ism")&&(n.equals("la1")||n.contains("vamos")))selected.add(c);}assertTrue(selected.size()>=2);try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("openMultiView",List.class);m.setAccessible(true);m.invoke(a,new ArrayList<>(selected.subList(0,2)));}catch(Exception e){throw new AssertionError(e);}});
   video(s); sharedImage(s);s.onActivity(a->{for(PlayerController p:(List<PlayerController>)f(a,"multiPlayerControllers"))players.add(f(p,"player"));});
   key(s,KeyEvent.KEYCODE_DPAD_RIGHT);s.onActivity(a->{assertEquals(1,f(a,"multiViewFocusedIndex"));assertEquals(0,f(a,"multiViewActiveIndex"));});
   key(s,KeyEvent.KEYCODE_DPAD_CENTER);s.onActivity(a->assertEquals(1,f(a,"multiViewActiveIndex")));
   key(s,KeyEvent.KEYCODE_DPAD_CENTER);s.onActivity(a->assertEquals(1,f(a,"multiViewExpandedIndex")));Thread.sleep(3000);
   android.util.Log.i("SharedMultiTest","back to mosaic");
   key(s,KeyEvent.KEYCODE_BACK);s.onActivity(a->{assertEquals(-1,f(a,"multiViewExpandedIndex"));List<PlayerController> list=(List<PlayerController>)f(a,"multiPlayerControllers");for(int i=0;i<2;i++)assertSame("Must keep decoder/player",players.get(i),f(list.get(i),"player"));});video(s); sharedImage(s);
   sample(s,"shared");
   // The classic path remains usable without rebuilding or changing channels.
   s.onActivity(a->{PlaybackComparisonDeviceTest.call(a,"stopSharedMultiComposition");PlaybackComparisonDeviceTest.call(a,"updateMultiViewFocus");});video(s);sample(s,"classic");
   s.onActivity(a->PlaybackComparisonDeviceTest.call(a,"startSharedMultiComposition"));video(s);sharedImage(s);
   android.util.Log.i("PlaybackCompare","multiview two videos, independent focus/audio, expanded/back, retained players: PASS");
   s.onActivity(a->PlaybackComparisonDeviceTest.call(a,"closeMultiView"));
  }
 }
}
