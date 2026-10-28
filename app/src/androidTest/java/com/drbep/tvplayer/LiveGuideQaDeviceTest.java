package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
import androidx.test.core.app.ActivityScenario;import androidx.test.platform.app.InstrumentationRegistry;
import android.view.KeyEvent;
public class LiveGuideQaDeviceTest {
 @Test public void daysSelectionDetailsAndReturnWithoutNetwork(){
  android.app.Instrumentation ins=InstrumentationRegistry.getInstrumentation();
  try(ActivityScenario<LiveGuideQaActivity> sc=ActivityScenario.launch(LiveGuideQaActivity.class)){
   ins.waitForIdleSync();sc.onActivity(a->assertEquals(24,a.index));
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);ins.waitForIdleSync();sc.onActivity(a->assertEquals(25,a.index));
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);ins.waitForIdleSync();sc.onActivity(a->{assertEquals(a.today.plusDays(1),a.day);assertEquals(24,a.index);});
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);ins.waitForIdleSync();sc.onActivity(a->{assertEquals(a.today,a.day);assertEquals(25,a.index);});
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);ins.waitForIdleSync();sc.onActivity(a->assertTrue(a.detail));
   ins.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);ins.waitForIdleSync();sc.onActivity(a->{assertFalse(a.detail);assertEquals(25,a.index);});
  }
 }
}
