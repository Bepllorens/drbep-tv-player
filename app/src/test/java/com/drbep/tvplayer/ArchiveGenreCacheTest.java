package com.drbep.tvplayer;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class ArchiveGenreCacheTest {
 @Test public void expiresAndSeparatesAccounts() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",100,Arrays.asList("Drama"));
  assertEquals(Arrays.asList("Drama"),c.get("a",15099));
  assertNull(c.get("b",101));assertNull(c.get("a",15100));
 }
 @Test public void firstEmptyCatalogIsNotCached() {
  ArchiveGenreCache c=new ArchiveGenreCache();c.put("a",0,Collections.emptyList());
  assertNull(c.get("a",1));
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
}
