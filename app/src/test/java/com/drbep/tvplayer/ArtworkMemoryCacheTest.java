package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class ArtworkMemoryCacheTest {
 @Test public void lruRetainsMostRecentlyUsedGrid(){
  ArtworkMemoryCache<String> c=new ArtworkMemoryCache<>(2);long g=c.generation();
  c.putIfCurrent("a","A",g);c.putIfCurrent("b","B",g);assertEquals("A",c.get("a"));
  c.putIfCurrent("c","C",g);assertNull(c.get("b"));assertEquals(2,c.size());
 }
 @Test public void inFlightResultCannotRepopulateAfterTrim(){
  ArtworkMemoryCache<String> c=new ArtworkMemoryCache<>(32);long old=c.generation();
  c.putIfCurrent("a","A",old);c.clear();c.putIfCurrent("late","Late",old);
  assertEquals(0,c.size());assertNull(c.get("a"));assertNull(c.get("late"));
 }
 @Test public void newRequestsCanCacheAfterTrim(){
  ArtworkMemoryCache<String> c=new ArtworkMemoryCache<>(32);long old=c.generation();c.clear();
  c.putIfCurrent("a","New",c.generation());c.putIfCurrent("a","Old",old);
  assertEquals("New",c.get("a"));
 }
 @Test public void repeatedTrimsRejectEveryEarlierGeneration(){
  ArtworkMemoryCache<String> c=new ArtworkMemoryCache<>(2);long first=c.generation();c.clear();long second=c.generation();c.clear();
  c.putIfCurrent("a","First",first);c.putIfCurrent("b","Second",second);assertEquals(0,c.size());
 }
 @Test(expected=IllegalArgumentException.class) public void capacityMustBePositive(){new ArtworkMemoryCache<String>(0);}
}
