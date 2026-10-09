package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class SleepTimerStateTest {
 @Test public void warnsOnceThenExpiresOnce() {
  SleepTimerState t = new SleepTimerState(); t.start(1000,1);
  assertEquals(SleepTimerState.Event.NONE,t.poll(30000));
  assertEquals(SleepTimerState.Event.WARN,t.poll(31000));
  assertEquals(SleepTimerState.Event.NONE,t.poll(32000));
  assertEquals(SleepTimerState.Event.EXPIRE,t.poll(61000));
  assertFalse(t.active()); assertEquals(SleepTimerState.Event.NONE,t.poll(62000));
 }
 @Test public void lateTickExpiresImmediately() {
  SleepTimerState t = new SleepTimerState();t.start(1000,15);
  assertEquals(SleepTimerState.Event.EXPIRE,t.poll(3600000));
 }
 @Test public void extendUsesRemainingTimeAndRearmsWarning() {
  SleepTimerState t = new SleepTimerState();t.start(1000,1);t.poll(31000);t.extend(31000,15);
  assertEquals(930000,t.remaining(31000));
  assertEquals(SleepTimerState.Event.NONE,t.poll(32000));
  assertEquals(SleepTimerState.Event.WARN,t.poll(931000));
 }
 @Test public void cancelCannotStopLaterPlayback() {
  SleepTimerState t = new SleepTimerState();t.start(1000,1);t.cancel();
  assertEquals(0,t.remaining(2000));assertEquals(SleepTimerState.Event.NONE,t.poll(999999));
 }
 @Test public void replacementAndExtensionStayBounded() {
  SleepTimerState t = new SleepTimerState();t.start(0,120);t.extend(100,120);
  assertEquals(7200000,t.remaining(100));t.start(100,15);assertEquals(900000,t.remaining(100));
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsZero() {new SleepTimerState().start(0,0);}
 @Test public void newSessionHasNoTimer() {assertFalse(new SleepTimerState().active());}
}
