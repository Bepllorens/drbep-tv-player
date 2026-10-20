package com.drbep.tvplayer;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class ArchiveGenreCacheTest {
 @Test public void expiresAndSeparatesAccounts() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",100,Arrays.asList("Drama"));
  assertEquals(Arrays.asList("Drama"),c.get("a",15099));
  assertNull(c.get("b",101));assertNull(c.get("a",15100));
 }
 @Test public void emptyCatalogIsCachedUntilExpiryOrWrite() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Collections.emptyList());
  assertEquals(Collections.emptyList(),c.get("a",1));
  assertNull(c.get("a",15000));
  c.put("a",16000,Collections.emptyList());c.invalidate("a");assertNull(c.get("a",16001));
 }
 @Test public void refreshReplacesGenresWithoutChangingEarlierSnapshot() {
  ArchiveGenreCache c=new ArchiveGenreCache();List<String> input=new ArrayList<>(Arrays.asList("Drama"));
  c.put("a",0,input);List<String> old=c.get("a",1);input.clear();
  c.put("a",15000,Arrays.asList("Comedia"));
  assertEquals(Arrays.asList("Drama"),old);assertEquals(Arrays.asList("Comedia"),c.get("a",15001));
 }

 @org.junit.Test public void staleSnapshotRemainsAvailableOnlyForItsAccount() {
  ArchiveGenreCache cache=new ArchiveGenreCache();
  cache.put("a",0,java.util.Arrays.asList("Drama"));
  org.junit.Assert.assertNull(cache.get("a",15000));
  org.junit.Assert.assertEquals(java.util.Arrays.asList("Drama"),cache.peek("a"));
  org.junit.Assert.assertNull(cache.peek("b"));
  cache.put("a",16000,java.util.Arrays.asList("Drama","Sport"));
  org.junit.Assert.assertEquals(2,cache.peek("a").size());
 }

 @Test public void concurrentReadersShareOneSuccessfulQuery() throws Exception {
  ArchiveGenreCache c=new ArchiveGenreCache();
  java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(6);
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  ArchiveGenreCache.Loader loader=()->{calls.incrementAndGet();entered.countDown();try {
   if(!release.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
  return Arrays.asList("Drama");};
  try {
   List<java.util.concurrent.Future<List<String>>> futures=new ArrayList<>();
   futures.add(pool.submit(()->c.getOrLoad("a",()->100,loader)));
   assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
   for(int i=0;i<5;i++)futures.add(pool.submit(()->c.getOrLoad("a",()->100,loader)));
   release.countDown();
   for(java.util.concurrent.Future<List<String>> f:futures)assertEquals(Arrays.asList("Drama"),f.get(3,java.util.concurrent.TimeUnit.SECONDS));
   assertEquals(1,calls.get());
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test public void staleSnapshotCanBeReadWhileQueryRuns() throws Exception {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Arrays.asList("Drama"));
  java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newSingleThreadExecutor();
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  try {
   java.util.concurrent.Future<?> work=pool.submit(()->c.getOrLoad("a",()->16000,()->{
    entered.countDown();try{if(!release.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
    return Arrays.asList("Sport");
   }));
   assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
   assertEquals(Arrays.asList("Drama"),c.peek("a"));assertNull(c.get("a",16000));
   release.countDown();work.get(3,java.util.concurrent.TimeUnit.SECONDS);
   assertEquals(Arrays.asList("Sport"),c.peek("a"));
  }finally{release.countDown();pool.shutdownNow();}
 }
 @Test public void failedQueryDoesNotReplaceSnapshotAndCanRetry() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Arrays.asList("Drama"));
  try {c.getOrLoad("a",()->16000,()->{throw new IllegalStateException("synthetic");});fail();}
  catch(IllegalStateException expected){}
  assertEquals(Arrays.asList("Drama"),c.peek("a"));
  assertEquals(Arrays.asList("Sport"),c.getOrLoad("a",()->16000,()->Arrays.asList("Sport")));
 }
 @Test public void freshnessStartsWhenSlowQueryFinishesAndAccountsStaySeparate() {
  ArchiveGenreCache c=new ArchiveGenreCache();long[] now={0};
  c.getOrLoad("a",()->now[0],()->{now[0]=20000;return Arrays.asList("Drama");});
  assertEquals(Arrays.asList("Drama"),c.get("a",34999));assertNull(c.get("a",35000));
  assertEquals(Arrays.asList("Sport"),c.getOrLoad("b",()->20001,()->Arrays.asList("Sport")));
  assertNull(c.peek("a"));
 }

 @Test public void repeatedEmptyQueriesShareResult() {
  ArchiveGenreCache c=new ArchiveGenreCache();int[] calls={0};
  for(int i=0;i<100;i++)assertTrue(c.getOrLoad("a",()->100,()->{calls[0]++;return Collections.emptyList();}).isEmpty());
  assertEquals(1,calls[0]);
  c.invalidate("a");
  assertEquals(Arrays.asList("New"),c.getOrLoad("a",()->101,()->Arrays.asList("New")));
 }
 @Test public void otherAccountWriteKeepsCurrentSnapshotFresh() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Arrays.asList("Drama"));
  c.invalidate("b");assertEquals(Arrays.asList("Drama"),c.get("a",1));
 }
 @Test public void writeDuringLoadDoesNotCacheOlderResultAsFresh() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Arrays.asList("Old"));
  c.getOrLoad("a",()->16000,()->{c.invalidate("a");return Arrays.asList("Outdated");});
  assertNull(c.get("a",16001));assertEquals(Arrays.asList("Old"),c.peek("a"));
  assertEquals(Arrays.asList("New"),c.getOrLoad("a",()->16001,()->Arrays.asList("New")));
  assertEquals(Arrays.asList("New"),c.get("a",16002));
 }

 @Test public void unrelatedWriteDuringLoadDoesNotForceAnotherQuery() {
  ArchiveGenreCache c=new ArchiveGenreCache();int[] calls={0};
  ArchiveGenreCache.Loader loader=()->{calls[0]++;c.invalidate("other");return Arrays.asList("Drama");};
  for(int i=0;i<100;i++)assertEquals(Arrays.asList("Drama"),c.getOrLoad("a",()->100,loader));
  assertEquals(1,calls[0]);
 }
 @Test public void matchingWriteStillInvalidatesAfterUnrelatedWrites() {
  ArchiveGenreCache c=new ArchiveGenreCache();
  c.getOrLoad("a",()->100,()->{c.invalidate("other");c.invalidate("a");c.invalidate("third");return Arrays.asList("Old");});
  assertNull(c.get("a",101));
  assertEquals(Arrays.asList("New"),c.getOrLoad("a",()->101,()->Arrays.asList("New")));
 }
 @Test public void failedLoadClearsActiveScopeBeforeNextAccount() {
  ArchiveGenreCache c=new ArchiveGenreCache();
  try {c.getOrLoad("a",()->100,()->{throw new IllegalStateException();});fail();}
  catch(IllegalStateException expected){}
  c.getOrLoad("b",()->101,()->{c.invalidate("a");return Arrays.asList("B");});
  assertEquals(Arrays.asList("B"),c.get("b",102));
 }
 @Test public void actualConcurrentUnrelatedWriteKeepsLoadedResult() throws Exception {
  ArchiveGenreCache c=new ArchiveGenreCache();
  java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newSingleThreadExecutor();
  java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
  try {
   java.util.concurrent.Future<?> work=pool.submit(()->c.getOrLoad("a",()->100,()->{
    entered.countDown();
    try {if(!release.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}
    return Arrays.asList("A");
   }));
   assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
   c.invalidate("b");release.countDown();work.get(3,java.util.concurrent.TimeUnit.SECONDS);
   assertEquals(Arrays.asList("A"),c.get("a",101));
  }finally{release.countDown();pool.shutdownNow();}
 }
}
