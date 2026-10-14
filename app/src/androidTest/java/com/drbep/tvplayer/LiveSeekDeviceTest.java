package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class LiveSeekDeviceTest {
 @Test public void upNeverEntersArchiveImplicitly()throws Exception{
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(8000);
   sc.onActivity(a->{try{
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playerController");f.setAccessible(true);PlayerController pc=(PlayerController)f.get(a);
    PlayerController.PlaybackSeekState seek=pc.getPlaybackSeekState();
    android.util.Log.i("LiveSeekTest","seek="+(seek!=null)+" dvr="+pc.isTimeshiftSupportedForCurrentChannel());

   }catch(Exception e){throw new AssertionError(e);}});
   // Directly test the interceptor independently of overlays and current provider capabilities.
   sc.onActivity(a->{try{
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("archiveStripFocused");f.setAccessible(true);f.setBoolean(a,false);
    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("handleArchiveStripKey",android.view.KeyEvent.class);m.setAccessible(true);
    assertFalse((Boolean)m.invoke(a,new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_UP)));
    assertFalse(f.getBoolean(a));
   }catch(Exception e){throw new AssertionError(e);}});
  }
 }
 @Test public void ismLiveCanRewindAndReturnLive()throws Exception { validateLive("ism"); }
 @Test public void hlsLiveCanRewindAndReturnLive()throws Exception { validateLive("hls"); }
 private void validateLive(String platform)throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(6000);
   sc.onActivity(a->{try {
    ComposeDialogHost.dismissAll(a);
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("allChannels");f.setAccessible(true);
    ChannelItem candidate=null;
    for(ChannelItem c:(java.util.List<ChannelItem>)f.get(a)) if(c.platformName.toLowerCase(java.util.Locale.ROOT).contains(platform) && FanartRepository.channelKey(c.name).equals("la1")){candidate=c;break;}
    assertNotNull("LA 1 ISM is required for native DVR validation",candidate);
    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("tuneChannelById",String.class);m.setAccessible(true);m.invoke(a,candidate.id);
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(12000);
   final long[] before={0};
   sc.onActivity(a->{try{
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playerController");f.setAccessible(true);PlayerController pc=(PlayerController)f.get(a);
    java.lang.reflect.Field pf=PlayerController.class.getDeclaredField("player");pf.setAccessible(true);
    androidx.media3.exoplayer.ExoPlayer ex=(androidx.media3.exoplayer.ExoPlayer)pf.get(pc);
    android.util.Log.i("LiveSeekTest","platform="+platform+" player="+(ex!=null)+" live="+(ex!=null&&ex.isCurrentMediaItemLive())+" seekable="+(ex!=null&&ex.isCurrentMediaItemSeekable())+" duration="+(ex==null?0:ex.getDuration()));
    java.lang.reflect.Field df=PlayerController.class.getDeclaredField("currentPlaybackDecision");df.setAccessible(true);Object decision=df.get(pc);
    for(String field:new String[]{"targetUrl","mimeType","route"}){try{java.lang.reflect.Field q=decision.getClass().getDeclaredField(field);q.setAccessible(true);Object v=q.get(decision);if(field.equals("targetUrl"))v=android.net.Uri.parse(String.valueOf(v)).getPath();android.util.Log.i("LiveSeekTest",field+"="+v);}catch(Exception ignored){}}
    android.util.Log.i("LiveSeekTest","state="+ex.getPlaybackState()+" playing="+ex.isPlaying()+" manifest="+(ex.getCurrentManifest()==null?"null":ex.getCurrentManifest().getClass().getSimpleName()));
    PlayerController.PlaybackSeekState state=pc.getPlaybackSeekState();assertNotNull(platform+" should expose a seek window",state);
    assertTrue("Native live DVR available",pc.isTimeshiftSupportedForCurrentChannel());before[0]=state.currentMs;
    if(platform.equals("hls")) {
     ComposeDialogHost.dismissAll(a);
     ((android.view.View)CatchupFocusDeviceTest.field(a,"touchHomeHub")).setVisibility(android.view.View.GONE);
     CatchupFocusDeviceTest.call(a,"showTouchControlsTemporarily");
     CatchupFocusDeviceTest.call(a,"focusTouchControlsTimeshift");
     CatchupFocusDeviceTest.key(a,android.view.KeyEvent.KEYCODE_DPAD_LEFT);
     assertEquals("One press moves 30 seconds, not to start",before[0]-30000,pc.getPlaybackSeekState().currentMs,1000);
     for(int n=0;n<5;n++)CatchupFocusDeviceTest.key(a,android.view.KeyEvent.KEYCODE_DPAD_LEFT);
     assertEquals("Six presses move exactly three minutes",before[0]-180000,pc.getPlaybackSeekState().currentMs,1000);
     CatchupFocusDeviceTest.key(a,android.view.KeyEvent.KEYCODE_DPAD_RIGHT);
     assertEquals("Right moves forward from the selected cursor",before[0]-150000,pc.getPlaybackSeekState().currentMs,1000);
    } else assertTrue("Rewind accepted",pc.seekTimeshiftBack());
   }catch(Exception e){throw new AssertionError(e);}});
   final boolean[] rewound={false};
   for(int i=0;i<120;i++) {
    sc.onActivity(a->{try{
     java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playerController");f.setAccessible(true);PlayerController pc=(PlayerController)f.get(a);
     java.lang.reflect.Field pf=PlayerController.class.getDeclaredField("player");pf.setAccessible(true);androidx.media3.exoplayer.ExoPlayer ex=(androidx.media3.exoplayer.ExoPlayer)pf.get(pc);
     PlayerController.PlaybackSeekState st=pc.getPlaybackSeekState();
     androidx.media3.exoplayer.DecoderCounters counters=ex.getVideoDecoderCounters();if(counters!=null)counters.ensureUpdated();
     java.lang.reflect.Field pending=PlayerController.class.getDeclaredField("hlsDvrPreview");pending.setAccessible(true);
     rewound[0]=pending.getLong(pc)<0&&st!=null&&st.currentMs<before[0]-10000&&ex.isPlaying()&&ex.getCurrentPosition()>1000&&counters!=null&&counters.renderedOutputBufferCount>5;
    }catch(Exception e){throw new AssertionError(e);}});
    if(rewound[0])break;Thread.sleep(250);
   }
   assertTrue("Rewound video must actually render and advance",rewound[0]);
   sc.onActivity(a->{try{
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playerController");f.setAccessible(true);PlayerController pc=(PlayerController)f.get(a);
    assertTrue("Playback position moved backwards",pc.getPlaybackSeekState().currentMs<before[0]-10000);
    assertTrue("Return to live accepted",pc.resumeTimeshiftLive());
   }catch(Exception e){throw new AssertionError(e);}});
   final boolean[] live={false};
   for(int i=0;i<120;i++) {
    sc.onActivity(a->{try{
     java.lang.reflect.Field f=MainActivity.class.getDeclaredField("playerController");f.setAccessible(true);PlayerController pc=(PlayerController)f.get(a);
     java.lang.reflect.Field pf=PlayerController.class.getDeclaredField("player");pf.setAccessible(true);androidx.media3.exoplayer.ExoPlayer ex=(androidx.media3.exoplayer.ExoPlayer)pf.get(pc);
     PlayerController.PlaybackSeekState st=pc.getPlaybackSeekState();
     java.lang.reflect.Field pending=PlayerController.class.getDeclaredField("hlsDvrPreview");pending.setAccessible(true);
     live[0]=pending.getLong(pc)<0&&st!=null&&st.endMs-st.currentMs<15000&&ex.isPlaying()&&ex.getCurrentPosition()>1000;
    }catch(Exception e){throw new AssertionError(e);}});
    if(live[0])break;Thread.sleep(250);
   }
   assertTrue("Return to live must render near the live edge",live[0]);
  }
 }
}
