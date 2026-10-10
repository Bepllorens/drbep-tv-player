package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class MovistarPlusCatchupDeviceTest {
 @Test public void plusArchiveAndPlayerStrip() throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(10000);final ChannelItem[] channel={null};final MainActivity[] activity={null};
   sc.onActivity(a->{try{activity[0]=a; for(ChannelItem c:(java.util.List<ChannelItem>)CatchupFocusDeviceTest.field(a,"allChannels"))if(c.platformName.equalsIgnoreCase("Movistar ISM")&&c.name.equalsIgnoreCase("Movistar Plus"))channel[0]=c;assertNotNull(channel[0]);
    System.out.println("Plus channel="+channel[0].id+" tvg="+channel[0].tvgId+" supports="+U7dChannelPolicy.supports(channel[0]));
    ComposeDialogHost.dismissAll(a);
    java.lang.reflect.Method tune=MainActivity.class.getDeclaredMethod("tuneChannelById",String.class);tune.setAccessible(true);tune.invoke(a,channel[0].id);
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(2000);
   java.lang.reflect.Method fetch=MainActivity.class.getDeclaredMethod("fetchMovistarIsmU7dProgramsFromBackend",ChannelItem.class);fetch.setAccessible(true);
   java.util.List<EpgRepository.EpgProgram> programs=(java.util.List<EpgRepository.EpgProgram>)fetch.invoke(activity[0],channel[0]);
   System.out.println("Plus direct catalogue="+programs.size());
   sc.onActivity(a->{try {java.lang.reflect.Method load=MainActivity.class.getDeclaredMethod("loadArchiveCatalog",boolean.class);load.setAccessible(true);load.invoke(a,true);}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(18000);
   sc.onActivity(a->{try{
    java.util.List<U7dCatalogEntry> strip=(java.util.List<U7dCatalogEntry>)CatchupFocusDeviceTest.call(a,"archiveStrip");
    System.out.println("Plus strip="+strip.size()+" active="+((ChannelItem)CatchupFocusDeviceTest.call(a,"getCurrentPlaybackChannelItem")).id);
    assertFalse("Movistar Plus must have catchup",strip.isEmpty());
    CatchupFocusDeviceTest.call(a,"showTouchControlsTemporarily");
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(1200);
   android.graphics.Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
   try(java.io.FileOutputStream o=InstrumentationRegistry.getInstrumentation().getTargetContext().openFileOutput("plus698.png",0)){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}finally{b.recycle();}
  }
 }
}
