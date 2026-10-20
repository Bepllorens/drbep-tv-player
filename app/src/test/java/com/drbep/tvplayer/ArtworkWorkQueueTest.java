package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class ArtworkWorkQueueTest {
 @Test public void saturatedQueueRejectsWithoutRunningOnCallerAndLaterRecovers()throws Exception {
  ExecutorService workers=ArtworkWorkQueue.create(1,1);
  CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(2);
  AtomicInteger ran=new AtomicInteger();
  try {
   assertTrue(ArtworkWorkQueue.submit(workers,()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{done.countDown();}}));
   assertTrue(started.await(2,TimeUnit.SECONDS));
   assertTrue(ArtworkWorkQueue.submit(workers,()->{ran.incrementAndGet();done.countDown();}));
   assertFalse(ArtworkWorkQueue.submit(workers,()->ran.addAndGet(100)));
   assertEquals(0,ran.get());
   release.countDown();assertTrue(done.await(2,TimeUnit.SECONDS));assertEquals(1,ran.get());
   CountDownLatch again=new CountDownLatch(1);
   assertTrue(ArtworkWorkQueue.submit(workers,again::countDown));assertTrue(again.await(2,TimeUnit.SECONDS));
  }finally{release.countDown();workers.shutdownNow();}
 }
 @Test public void closedQueueNeverRunsNewRequests(){
  ExecutorService workers=ArtworkWorkQueue.create(1,1);workers.shutdownNow();
  AtomicInteger ran=new AtomicInteger();assertFalse(ArtworkWorkQueue.submit(workers,ran::incrementAndGet));assertEquals(0,ran.get());
 }
 @Test public void lockRegistryIsBoundedAndStableForRepeatedChannelDay(){
  Object[] locks=ArtworkWorkQueue.locks(64);
  java.util.Set<Object> used=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
  for(int i=0;i<10000;i++){String key="channel-day-"+i;Object first=ArtworkWorkQueue.lock(locks,key);assertSame(first,ArtworkWorkQueue.lock(locks,key));used.add(first);}
  assertTrue(used.size()<=64);assertEquals(64,locks.length);
 }
 @Test public void invalidCapacityCannotCreateUnboundedQueue(){
  try{ArtworkWorkQueue.create(1,0);fail();}catch(IllegalArgumentException expected){}
  try{ArtworkWorkQueue.create(0,1);fail();}catch(IllegalArgumentException expected){}
 }
}
