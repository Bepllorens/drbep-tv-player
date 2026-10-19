package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class DeferredArchiveRefreshTest {
 @Test public void hiddenBatchesWaitAndNewestWins(){
  DeferredArchiveRefresh g=new DeferredArchiveRefresh();int[] count={0};
  for(int i=1;i<=178;i++){final int v=i;g.request(()->count[0]=v);g.drain(false);}
  assertEquals(0,count[0]);assertTrue(g.pending());g.drain(true);
  assertEquals(178,count[0]);assertFalse(g.pending());g.drain(true);assertEquals(178,count[0]);
 }
 @Test public void requestDuringDrainSurvives(){
  DeferredArchiveRefresh g=new DeferredArchiveRefresh();int[] count={0};
  g.request(()->{count[0]++;g.request(()->count[0]++);});g.drain(true);
  assertEquals(1,count[0]);assertTrue(g.pending());g.drain(true);assertEquals(2,count[0]);
 }
}
