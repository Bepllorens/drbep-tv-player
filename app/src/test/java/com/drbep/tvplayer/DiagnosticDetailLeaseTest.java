package com.drbep.tvplayer;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;
public class DiagnosticDetailLeaseTest {
 @Test public void observerExpiresWithoutBackgroundTimerAndOtherTabStaysActive(){
  AtomicLong now=new AtomicLong();DiagnosticDetailLease leases=new DiagnosticDetailLease(now::get);
  leases.update("tab-a",1,true);now.set(10000);leases.update("tab-b",1,true);
  now.set(20000);assertEquals(1,leases.activeCount());
  leases.update("tab-b",2,false);assertEquals(0,leases.activeCount());
 }
 @Test public void reorderedReleaseCannotCancelNewerObservationAndOldRenewCannotReviveIt(){
  DiagnosticDetailLease leases=new DiagnosticDetailLease(()->0L);
  leases.update("tab",3,true);leases.update("tab",2,false);assertEquals(1,leases.activeCount());
  leases.update("tab",4,false);leases.update("tab",3,true);assertEquals(0,leases.activeCount());
 }
 @Test public void renewalExtendsOnlyItsOwnTabAndClearStopsAll(){
  AtomicLong now=new AtomicLong();DiagnosticDetailLease leases=new DiagnosticDetailLease(now::get);
  leases.update("a",1,true);leases.update("b",1,true);now.set(19000);leases.update("a",2,true);
  now.set(20001);assertEquals(1,leases.activeCount());leases.clear();assertEquals(0,leases.activeCount());
 }
 @Test public void boundedObserversAndExpiredTombstonesAreReclaimed(){
  AtomicLong now=new AtomicLong();DiagnosticDetailLease leases=new DiagnosticDetailLease(now::get);
  for(int i=0;i<16;i++)assertTrue(leases.update("tab"+i,1,true));
  assertFalse(leases.update("overflow",1,true));
  now.set(120001);assertTrue(leases.update("new",1,true));assertEquals(1,leases.activeCount());
 }
 @Test(expected=IllegalArgumentException.class) public void rejectsMalformedClient(){
  new DiagnosticDetailLease().update("bad/client",1,true);
 }
}
