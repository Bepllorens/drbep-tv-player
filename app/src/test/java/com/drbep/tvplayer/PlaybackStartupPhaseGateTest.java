package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class PlaybackStartupPhaseGateTest {
 @Test public void requiresPreparationAndKnownPhase(){
  PlaybackStartupPhaseGate g=new PlaybackStartupPhaseGate();
  assertFalse(g.accept(1,1,1));g.begin(1,1);
  assertFalse(g.accept(1,1,0));assertFalse(g.accept(1,1,4));assertFalse(g.accept(0,1,1));
 }
 @Test public void eachPhaseIsRecordedOnlyOnce(){
  PlaybackStartupPhaseGate g=new PlaybackStartupPhaseGate();g.begin(1,4);
  for(int phase=1;phase<=3;phase++){assertTrue(g.accept(1,4,phase));for(int i=0;i<100;i++)assertFalse(g.accept(1,4,phase));}
 }
 @Test public void rejectsOldGenerationAndOperationWithoutConsumingCurrentPhase(){
  PlaybackStartupPhaseGate g=new PlaybackStartupPhaseGate();g.begin(12,8);
  assertFalse(g.accept(11,8,1));assertFalse(g.accept(12,7,1));assertTrue(g.accept(12,8,1));
 }
 @Test public void retryInSameGenerationHasItsOwnPhases(){
  PlaybackStartupPhaseGate g=new PlaybackStartupPhaseGate();g.begin(1,3);assertTrue(g.accept(1,3,2));
  g.begin(2,3);assertFalse(g.accept(1,3,2));assertTrue(g.accept(2,3,2));
 }
 @Test public void independentPlayersDoNotConsumeEachOthersEvents(){
  PlaybackStartupPhaseGate a=new PlaybackStartupPhaseGate(),b=new PlaybackStartupPhaseGate();a.begin(1,1);b.begin(2,1);
  assertFalse(a.accept(2,1,3));assertTrue(a.accept(1,1,3));assertTrue(b.accept(2,1,3));
 }
 @Test public void concurrentDuplicateCallbacksStillRecordOnce() throws Exception {
  PlaybackStartupPhaseGate g=new PlaybackStartupPhaseGate();g.begin(7,9);AtomicInteger accepted=new AtomicInteger();
  ExecutorService pool=Executors.newFixedThreadPool(4);CountDownLatch start=new CountDownLatch(1);
  try {Future<?>[] jobs=new Future<?>[4];for(int t=0;t<4;t++)jobs[t]=pool.submit(()->{try{start.await();}catch(InterruptedException e){throw new RuntimeException(e);}for(int i=0;i<1000;i++)if(g.accept(7,9,2))accepted.incrementAndGet();});
   start.countDown();for(Future<?> job:jobs)job.get(5,TimeUnit.SECONDS);assertEquals(1,accepted.get());
  }finally{pool.shutdownNow();}
 }
}
