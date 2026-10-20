package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class RemoteZapTraceTest {
 @Test public void attributesOnlyTheBoundAttemptAndOnlyOnce() {
  RemoteZapTrace t=new RemoteZapTrace();t.requested(7,100,350,"a");t.bind("a",12);
  assertNull(t.frame(11,400));
  RemoteZapTrace.Sample s=t.frame(12,900);
  assertEquals(7,s.operation);assertEquals(800,s.pressToFrameMs);assertEquals(550,s.tuneToFrameMs);
  assertNull(t.frame(12,1000));
 }
 @Test public void replacementRetryAndDifferentChannelsCannotClaimOldInput() {
  RemoteZapTrace t=new RemoteZapTrace();t.requested(1,100,100,"a");t.bind("b",1);
  assertNull(t.frame(1,300));
  t.requested(2,400,450,"a");t.bind("a",2);t.bind("a",3);
  assertNull(t.frame(2,600));assertNull(t.frame(3,600));
 }
 @Test public void abandonedOrCancelledDispatchCannotLeakToLaterPlayback() {
  RemoteZapTrace t=new RemoteZapTrace();t.requested(1,100,100,"a");t.discardUnclaimed();t.bind("a",1);
  assertNull(t.frame(1,500));
  t.requested(2,500,500,"a");t.bind("a",2);t.cancel();assertNull(t.frame(2,700));
 }
 @Test public void sameChannelOldMediaItemIsNotNewFirstFrame() {
  RemoteZapTrace t=new RemoteZapTrace();t.requested(1,100,100,"a");t.bind("a",1);
  t.requested(2,200,200,"a");t.bind("a",2);
  assertNull(t.frame(1,250));assertNotNull(t.frame(2,300));
 }
}
