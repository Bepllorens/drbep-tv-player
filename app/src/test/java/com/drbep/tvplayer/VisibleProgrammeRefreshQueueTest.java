package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisibleProgrammeRefreshQueueTest {
 @Test public void onlyMountedSubscribersRunAndRespectTheirInitialDelay() {
  VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(null); int[] calls={0,0};
  q.register(100,0,()->calls[0]++); q.register(100,10_000,()->calls[1]++);
  q.runDue(100); assertArrayEquals(new int[]{0,0},calls);
  q.resume(100); assertEquals(0,q.delayUntilNext(100)); q.runDue(100);
  assertArrayEquals(new int[]{1,0},calls); assertEquals(10_000,q.delayUntilNext(100));
  q.runDue(10_099); assertArrayEquals(new int[]{1,0},calls);
  q.runDue(10_100); assertArrayEquals(new int[]{1,1},calls);
  q.runDue(15_100); assertArrayEquals(new int[]{2,1},calls);
 }
 @Test public void pausingStopsReadsAndResumeRestartsInitialDelays() {
  VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(null);int[] calls={0};
  q.register(0,500,()->calls[0]++);q.resume(0);q.runDue(500);q.pause();
  q.runDue(100_000);assertEquals(1,calls[0]);assertEquals(Long.MAX_VALUE,q.delayUntilNext(100_000));
  q.resume(100_000);assertEquals(500,q.delayUntilNext(100_000));q.runDue(100_499);assertEquals(1,calls[0]);
  q.runDue(100_500);assertEquals(2,calls[0]);
 }
 @Test public void removedRowsNeverRefreshAndNoCatchUpBurstsOccur() {
  VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(null);int[] calls={0,0};
  VisibleProgrammeRefreshQueue.Registration old=q.register(0,0,()->calls[0]++);
  q.register(0,0,()->calls[1]++);q.resume(0);q.unregister(old);
  q.runDue(600_000);assertArrayEquals(new int[]{0,1},calls);
  assertEquals(15_000,q.delayUntilNext(600_000));q.runDue(600_001);
  assertArrayEquals(new int[]{0,1},calls);q.clear();
  assertEquals(0,q.size());assertEquals(Long.MAX_VALUE,q.delayUntilNext(600_001));
 }
 @Test public void disposalDuringABatchCancelsRemainingStaleCallbacks() {
  VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(null);int[] calls={0};
  VisibleProgrammeRefreshQueue.Registration[] next={null};
  q.register(0,0,()->q.unregister(next[0]));next[0]=q.register(0,0,()->calls[0]++);
  q.resume(0);q.runDue(0);assertEquals(0,calls[0]);assertEquals(1,q.size());
 }
 @Test public void failedReaderDoesNotStopOtherRowsOrTheirNextRefresh() {
  int[] failed={0},fresh={0};VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(e->failed[0]++);
  q.register(0,0,()->{throw new IllegalStateException();});q.register(0,0,()->fresh[0]++);
  q.resume(0);q.runDue(0);q.runDue(15_000);
  assertEquals(2,failed[0]);assertEquals(2,fresh[0]);
 }
 @Test public void newRowsWakeImmediatelyWithoutWaitingForTheExistingDeadline() {
  VisibleProgrammeRefreshQueue q=new VisibleProgrammeRefreshQueue(null);int[] calls={0};
  q.register(0,10_000,()->{});q.resume(0);q.register(500,0,()->calls[0]++);
  assertEquals(0,q.delayUntilNext(500));q.runDue(500);assertEquals(1,calls[0]);
  assertEquals(9_500,q.delayUntilNext(500));
 }
}
