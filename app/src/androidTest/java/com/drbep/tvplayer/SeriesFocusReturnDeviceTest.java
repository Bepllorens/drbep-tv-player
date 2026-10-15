package com.drbep.tvplayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.view.KeyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class SeriesFocusReturnDeviceTest extends MovieContinuousDeviceTest {
 @Test public void episodesRestoreAfterReturningToDetails()throws Exception {
  try(ActivityScenario<MainActivity> sc=ActivityScenario.launch(MainActivity.class)) {
   Thread.sleep(7000);
   sc.onActivity(a->{try{ComposeDialogHost.dismissAll(a);java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("showImportedSeries",String.class,Runnable.class);m.setAccessible(true);m.invoke(a,"",(Runnable)()->{});}catch(Exception e){throw new AssertionError(e);}});
   Thread.sleep(7000);
   android.accessibilityservice.AccessibilityServiceInfo service=ins.getUiAutomation().getServiceInfo();service.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;ins.getUiAutomation().setServiceInfo(service);Thread.sleep(1000);
   key(KeyEvent.KEYCODE_DPAD_CENTER);
   assertTrue("Series details: "+focus(),focus().contains("Temporadas y episodios"));
   key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(1800);
   assertFalse("Episode must receive focus",focus().isEmpty());
   assertFalse(focus().contains("Volver a la ficha"));
   key(KeyEvent.KEYCODE_DPAD_DOWN);String episode=focus();
   assertFalse(episode.isEmpty());
   key(KeyEvent.KEYCODE_BACK);
   assertTrue(focus().contains("Temporadas y episodios"));
   key(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(1500);
   assertEquals("Return to the selected episode",episode,focus());
   key(KeyEvent.KEYCODE_BACK);key(KeyEvent.KEYCODE_BACK);
   assertFalse("Catalogue restores focus",focus().isEmpty());
  }
 }
}
