package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.KeyEvent;
import java.util.*;
public class TimelineGuideDeviceTest {
 final android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
 @Test public void localStoreRetainsDaysAndSeparatesSources()throws Exception {
  android.content.Context context=ins.getTargetContext();context.deleteDatabase("timeline-epg-test.db");
  try(TimelineEpgStore store=new TimelineEpgStore(context,"timeline-epg-test.db")) {
   ChannelItem c=new ChannelItem("fixture","Test","","","","","",0,0,false,false,0,"",new ArrayList<>(),"","","",false);
   long now=System.currentTimeMillis();List<EpgRepository.EpgProgram> rows=new ArrayList<>();
   for(int i=-24;i<48;i++) rows.add(new EpgRepository.EpgProgram(c.id,c.name,"Programme "+i,"","",java.time.Instant.ofEpochMilli(now+i*3600000L).toString(),java.time.Instant.ofEpochMilli(now+(i+1)*3600000L).toString(),0));
   store.save(c,rows);assertEquals(72,store.read(c,now-24*3600000L,now+48*3600000L).size());assertTrue(store.fresh(c));
   assertEquals(3,store.read(c,now+24*3600000L,now+27*3600000L).size());
   List<EpgRepository.EpgProgram> full=store.read(c,now,now+48*3600000L);
   List<EpgRepository.EpgProgram> bounded=store.readUpcoming(c,now,now+48*3600000L,16);
   assertEquals(16,bounded.size());
   for(int i=0;i<bounded.size();i++) {
    assertEquals(full.get(i).startTime,bounded.get(i).startTime);
    assertEquals(full.get(i).title,bounded.get(i).title);
   }
   assertEquals(3,store.readUpcoming(c,now+24*3600000L,now+27*3600000L,40).size());
   assertTrue(store.readUpcoming(c,now+80*3600000L,now+81*3600000L,40).isEmpty());
   try {store.readUpcoming(c,now,now+3600000L,0);fail("Unbounded request rejected");}catch(IllegalArgumentException expected){}

   store.save(c,Collections.singletonList(new EpgRepository.EpgProgram(c.id,c.name,"Replacement","","",java.time.Instant.ofEpochMilli(now).toString(),java.time.Instant.ofEpochMilli(now+3*3600000L).toString(),0)));
   assertEquals(1,store.read(c,now,now+3*3600000L).size());
   ChannelItem other=new ChannelItem("other","Test","","","","","",0,0,false,false,0,"",new ArrayList<>(),"","","",false);
   assertTrue(store.read(other,0,Long.MAX_VALUE).isEmpty());
  } finally { context.deleteDatabase("timeline-epg-test.db"); }
 }
 @Test public void opensImmediatelyAndRefreshesSameDialog()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(8000);final Object[] dialog={null};final long[] elapsed={0};
   sc.onActivity(a->{try{
    ComposeDialogHost.dismissAll(a);
    long start=android.os.SystemClock.elapsedRealtime();CatchupFocusDeviceTest.call(a,"openTimelineGuideNow");elapsed[0]=android.os.SystemClock.elapsedRealtime()-start;
    dialog[0]=CatchupFocusDeviceTest.field(a,"activeTimelineDialog");assertNotNull(dialog[0]);assertTrue(((android.app.Dialog)dialog[0]).isShowing());
   }catch(Exception e){throw new AssertionError(e);}});
   assertTrue("Opening must not await network: "+elapsed[0],elapsed[0]<1500);
   Thread.sleep(7000);
   sc.onActivity(a->{try{assertSame(dialog[0],CatchupFocusDeviceTest.field(a,"activeTimelineDialog"));}catch(Exception e){throw new AssertionError(e);}});
   android.graphics.Bitmap shot=ins.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=ins.getTargetContext().openFileOutput("guide707.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
   final String[] focusBefore={null};
   sc.onActivity(a->{try{focusBefore[0]=(String)CatchupFocusDeviceTest.field(a,"activeTimelineAnchorChannelId");}catch(Exception e){throw new AssertionError(e);}});
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);Thread.sleep(400);
   sc.onActivity(a->{try{assertNotEquals("Down moves to another channel",focusBefore[0],CatchupFocusDeviceTest.field(a,"activeTimelineAnchorChannelId"));}catch(Exception e){throw new AssertionError(e);}});
   AccessibilityNodeInfo tomorrow=ImportedMoviesDeviceTest.findClickable(ins.getUiAutomation().getRootInActiveWindow(),"Mañana");assertNotNull("Day selector",tomorrow);assertTrue(tomorrow.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(700);
   sc.onActivity(a->{try{assertNotSame(dialog[0],CatchupFocusDeviceTest.field(a,"activeTimelineDialog"));long time=(Long)CatchupFocusDeviceTest.field(a,"activeTimelineWindowStartMs");assertEquals(java.time.LocalDate.now().plusDays(1),java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate());}catch(Exception e){throw new AssertionError(e);}});
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);Thread.sleep(300);
   assertNotNull(ins.getUiAutomation().getRootInActiveWindow());
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
   sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);CatchupFocusDeviceTest.call(a,"showOverlay");CatchupFocusDeviceTest.call(a,"toggleLiveChannelGuide");}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(1200);
   sc.onActivity(a->{android.view.View menu=a.findViewById(R.id.channelOverlay),guide=a.findViewById(R.id.liveChannelGuide);assertEquals(0f,menu.getX(),1f);assertTrue("Guide opens to the right",guide.getX()>=menu.getWidth());});
   shot=ins.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=ins.getTargetContext().openFileOutput("menu707.png",0)){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
   android.os.Bundle report=new android.os.Bundle();report.putString("stream","Timeline opened in "+elapsed[0]+" ms\n");ins.sendStatus(0,report);
  }
 }
}
