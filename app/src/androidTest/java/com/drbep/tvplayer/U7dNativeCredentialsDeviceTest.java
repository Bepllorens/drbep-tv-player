package com.drbep.tvplayer;

import android.net.Uri;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class U7dNativeCredentialsDeviceTest {
 static Object field(MainActivity a,String n)throws Exception {java.lang.reflect.Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 static Object call(MainActivity a,String n,Class<?>[] types,Object... args)throws Exception {java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(a,args);}
 static PlayerController controller(MainActivity a)throws Exception{return (PlayerController)field(a,"playerController");}
 static long awaitPlaying(ActivityScenario<MainActivity> scenario)throws Exception {
  final long[] pos={-1};
  for(int i=0;i<180;i++) {
   scenario.onActivity(a->{try {
    PlayerController.PlaybackDiagnostics d=controller(a).getPlaybackDiagnostics();
    String base=(String)field(a,"currentPlaybackU7dBaseUrl");
    assertEquals("Cached route must remain selected", "1",Uri.parse(base).getQueryParameter("native_channel"));
    if(d.playing && d.firstFrameRendered && controller(a).getU7dBufferedPlaybackPosition()>5000) pos[0]=controller(a).getU7dBufferedPlaybackPosition();
   }catch(Exception e){throw new AssertionError(e);}});
   if(pos[0]>=0)return pos[0];Thread.sleep(500);
  }
  fail("Replay did not render and advance within 90 seconds");return -1;
 }
 @Test public void cachedCredentialsRenderAndSeek()throws Exception {
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(9000);
   ChannelItem[] channel={null};String[] probe={null};EpgRepository.EpgProgram[] program={null};
   scenario.onActivity(a->{try {
    for(ChannelItem c:(List<ChannelItem>)field(a,"allChannels"))if(c.platformName.toLowerCase(java.util.Locale.ROOT).contains("hls")&&FanartRepository.channelKey(c.name).equals("la1")){channel[0]=c;break;}
    assertNotNull("La 1 HLS must exist",channel[0]);
    Instant start=Instant.now().minusSeconds(7200);Instant end=start.plusSeconds(3600);
    program[0]=new EpgRepository.EpgProgram(channel[0].id,channel[0].name,"Prueba U7D", "", "", start.toString(),end.toString(),0);
    call(a,"ensureArchiveScope",new Class[]{});
    List<EpgRepository.EpgProgram> cached=((U7dCatalogStore)field(a,"archiveStore")).read((String)field(a,"archiveScope"),channel[0].id);
    long distance=Long.MAX_VALUE;
    for(EpgRepository.EpgProgram p:cached) {
        long ps=Instant.parse(p.startTime).toEpochMilli(),pe=Instant.parse(p.endTime).toEpochMilli();
        long d=Math.abs(ps-start.toEpochMilli());
        if(pe<System.currentTimeMillis() && pe-ps>1200000L && d<distance){program[0]=p;distance=d;}
    }
    String base=(String)call(a,"buildMovistarIsmU7dUrl",new Class[]{ChannelItem.class,EpgRepository.EpgProgram.class},channel[0],program[0]);
    probe[0]=Uri.parse(base).buildUpon().appendQueryParameter("native_probe","1").build().toString();
   }catch(Exception e){throw new AssertionError(e);}});
   HttpURLConnection connection=(HttpURLConnection)new URL(probe[0]).openConnection();
   try {
    connection.setConnectTimeout(15000);connection.setReadTimeout(20000);assertEquals(200,connection.getResponseCode());
    java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
    try(java.io.InputStream input=connection.getInputStream()){while((n=input.read(buf))!=-1)bytes.write(buf,0,n);}
    JSONObject response=new JSONObject(bytes.toString("UTF-8"));
    assertEquals("Manifest must use existing credentials: "+response.optString("reason"), "channel",response.optString("route"));assertTrue(response.optBoolean("keys_verified"));
   }finally{connection.disconnect();}
   scenario.onActivity(a->{try {
    ComposeDialogHost.dismissAll(a);
    call(a,"playMovistarIsmU7dProgram",new Class[]{ChannelItem.class,EpgRepository.EpgProgram.class,long.class},channel[0],program[0],0L);
    assertEquals(android.view.View.GONE,a.findViewById(R.id.startupLoadingOverlay).getVisibility());
    assertEquals("Home must not cover loading thumbnail",android.view.View.GONE,((android.view.View)field(a,"touchHomeHub")).getVisibility());
    TouchControlsBarUiModel model=(TouchControlsBarUiModel)field(a,"currentTouchControlsBarModel");
    assertFalse("Ring must target the selected thumbnail",model.archiveLoadingId.isEmpty());
    assertTrue(model.archiveEntries.stream().anyMatch(e->e.id.equals(model.archiveLoadingId)));

   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(1000);
   android.graphics.Bitmap capture=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   java.io.File output=new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"u7d696-loading.png");
   try(java.io.FileOutputStream file=new java.io.FileOutputStream(output)){capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}capture.recycle();
   long before=awaitPlaying(scenario);
   scenario.onActivity(a->{try {call(a,"restartCurrentU7dPlaybackAt",new Class[]{long.class},900000L);}catch(Exception e){throw new AssertionError(e);}});
   long after=awaitPlaying(scenario);
   scenario.onActivity(a->{try {
    assertEquals(900000L,((Long)field(a,"currentPlaybackU7dOffsetMs")).longValue());
    assertTrue(controller(a).getPlaybackDiagnostics().lastError.isEmpty());
    call(a,"showTouchControlsTemporarily",new Class[]{});
    TouchControlsBarUiModel model=(TouchControlsBarUiModel)field(a,"currentTouchControlsBarModel");
    assertTrue("Ring must stop after first frame",model.archiveLoadingId.isEmpty());
    assertEquals("Volver al directo",model.actions.get(0).label);
    ((TouchControlsFocusState)field(a,"touchControlsFocusState")).focusAction(0);
    call(a,"refreshTouchControlsBar",new Class[]{});
    a.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_CENTER));
    a.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_DPAD_CENTER));
    assertNull("Remote OK must exit catchup",field(a,"currentPlaybackU7dChannel"));

   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(8000);
   scenario.onActivity(a->{try{assertTrue("Return to live must play",controller(a).isPlaying());}catch(Exception e){throw new AssertionError(e);}});
   android.util.Log.i("U7dNativeTest","Rendered replay position="+before+"ms; seek 15min rendered position="+after+"ms; returned live");
  }
 }
}
