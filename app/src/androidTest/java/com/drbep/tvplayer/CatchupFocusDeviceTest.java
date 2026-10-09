package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;import org.junit.runner.RunWith;
import android.view.KeyEvent;import android.view.View;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class CatchupFocusDeviceTest {
 static Object field(MainActivity a,String name)throws Exception{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(a);}
 static Object call(MainActivity a,String name)throws Exception{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);return m.invoke(a);}
 static void key(MainActivity a,int code){a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,code));a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,code));}
 @Test public void actionsTimelineCatchupRoundTrip()throws Exception{
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)){
   Thread.sleep(7000);
   sc.onActivity(a->{try{
    ChannelItem channel=null;
    for(ChannelItem c:(java.util.List<ChannelItem>)field(a,"allChannels"))if(c.platformName.toLowerCase(java.util.Locale.ROOT).contains("ism")&&FanartRepository.channelKey(c.name).equals("la1")){channel=c;break;}
    assertNotNull(channel);
    java.lang.reflect.Method tune=MainActivity.class.getDeclaredMethod("tuneChannelById",String.class);tune.setAccessible(true);tune.invoke(a,channel.id);
    java.lang.reflect.Method load=MainActivity.class.getDeclaredMethod("loadArchiveCatalog",boolean.class);load.setAccessible(true);load.invoke(a,false);
   }catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(12000);
   sc.onActivity(a->{try{
    ((View)field(a,"touchHomeHub")).setVisibility(View.GONE);
    call(a,"showTouchControlsTemporarily");
    assertFalse((Boolean)call(a,"hasBlockingOverlaySurfaceVisible"));
    assertFalse(((java.util.List<?>)call(a,"archiveStrip")).isEmpty());
    key(a,KeyEvent.KEYCODE_DPAD_UP);
    assertTrue((Boolean)call(a,"isTouchControlsTimeshiftFocused"));assertFalse((Boolean)field(a,"archiveStripFocused"));
    key(a,KeyEvent.KEYCODE_DPAD_UP);assertTrue((Boolean)field(a,"archiveStripFocused"));
    java.util.List<?> entries=(java.util.List<?>)call(a,"archiveStrip");
    assertEquals("First entry must select latest programme",entries.size()-1,((Integer)field(a,"archiveStripIndex")).intValue());
    key(a,KeyEvent.KEYCODE_DPAD_LEFT);int selected=(Integer)field(a,"archiveStripIndex");
    key(a,KeyEvent.KEYCODE_DPAD_DOWN);key(a,KeyEvent.KEYCODE_DPAD_UP);
    assertEquals("Re-enter keeps selection",selected,((Integer)field(a,"archiveStripIndex")).intValue());
    key(a,KeyEvent.KEYCODE_DPAD_DOWN);assertFalse((Boolean)field(a,"archiveStripFocused"));assertTrue((Boolean)call(a,"isTouchControlsTimeshiftFocused"));
    key(a,KeyEvent.KEYCODE_DPAD_DOWN);assertFalse((Boolean)call(a,"isTouchControlsTimeshiftFocused"));
   }catch(Exception e){throw new AssertionError(e);}});
  }
 }
}
