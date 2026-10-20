package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.*;
/** Holds the actual loading monitor without making any network request. */
public class TimelineDetailsCacheConcurrencyTest {
 private static final String START="2026-10-20T10:00:00Z";
 @SuppressWarnings("unchecked")
 private static void cached(String code,ProgrammeMetadata value) throws Exception {
  Field f=TimelineProgrammeDetails.class.getDeclaredField("CACHE");f.setAccessible(true);
  Map<String,Object> cache=(Map<String,Object>)f.get(null);
  Class<?> type=Class.forName("com.drbep.tvplayer.TimelineProgrammeDetails$CachedDescription");
  Constructor<?> ctor=type.getDeclaredConstructor(ProgrammeMetadata.class,boolean.class);ctor.setAccessible(true);
  synchronized(cache){if(value==null)cache.remove(TimelineProgrammeDetails.key(code,START));else cache.put(TimelineProgrammeDetails.key(code,START),ctor.newInstance(value,true));}
 }
 private interface Task {void run() throws Exception;}
 private static void whileLoading(Task task) throws Exception {
  CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
  Thread blocker=new Thread(()->{synchronized(TimelineProgrammeDetails.class){held.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}});
  blocker.start();assertTrue(held.await(2,TimeUnit.SECONDS));
  ExecutorService executor=Executors.newSingleThreadExecutor();
  try {Future<?> work=executor.submit(()->{task.run();return null;});work.get(2,TimeUnit.SECONDS);}
  finally{release.countDown();executor.shutdownNow();blocker.join(2000);}
 }
 @Test public void cachedMetadataDoesNotWaitForOtherProgramme() throws Exception {
  ProgrammeMetadata expected=new ProgrammeMetadata("Cine","2020","12","7","Synopsis");
  cached("CACHE_A",expected);
  try{whileLoading(()->assertSame(expected,TimelineProgrammeDetails.loadMetadata("CACHE_A",START,"")));}
  finally{cached("CACHE_A",null);}
 }
 @Test public void invalidIdentityDoesNotWaitOrLoad() throws Exception {
  whileLoading(()->assertSame(ProgrammeMetadata.EMPTY,TimelineProgrammeDetails.loadMetadata("../bad",START,"")));
 }
 @Test public void cancellationStillWinsOverCacheHit() throws Exception {
  cached("CACHE_B",ProgrammeMetadata.EMPTY);
  try{whileLoading(()->{Thread.currentThread().interrupt();try{TimelineProgrammeDetails.loadMetadata("CACHE_B",START,"");fail("Cancelled request returned a result");}catch(InterruptedException expected){}finally{Thread.interrupted();}});}
  finally{cached("CACHE_B",null);}
 }
 @Test public void waitingMissRechecksCacheBeforeLoading() throws Exception {
  ProgrammeMetadata expected=new ProgrammeMetadata("Series","2024","","","Shared");
  java.util.concurrent.atomic.AtomicReference<ProgrammeMetadata> result=new java.util.concurrent.atomic.AtomicReference<>();
  java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
  Thread waiter=new Thread(()->{try{result.set(TimelineProgrammeDetails.loadMetadata("CACHE_C",START,""));}catch(Throwable e){error.set(e);}});
  try {
   synchronized(TimelineProgrammeDetails.class) {
    waiter.start();long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
    while(waiter.getState()!=Thread.State.BLOCKED && System.nanoTime()<end)Thread.sleep(5);
    if(waiter.getState()!=Thread.State.BLOCKED){waiter.interrupt();fail("Miss did not reach serialized loader");}
    cached("CACHE_C",expected);
   }
   waiter.join(2000);assertFalse(waiter.isAlive());assertNull(error.get());assertSame(expected,result.get());
  } finally {waiter.interrupt();waiter.join(2000);cached("CACHE_C",null);}
 }
}
