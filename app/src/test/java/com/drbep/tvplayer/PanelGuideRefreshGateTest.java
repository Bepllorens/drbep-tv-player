package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PanelGuideRefreshGateTest {
 @Test public void repeatedPollingDoesNotStartAnotherFetch() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket t=g.begin("scope:1:day:all",0);
  assertNotNull(t);assertNull(g.begin("scope:1:day:all",1));assertTrue(g.busy());
  g.complete(t,100,true);assertFalse(g.busy());assertNull(g.begin(t.key,60099));
  assertNotNull(g.begin(t.key,60100));
 }
 @Test public void atMostTwoDifferentRequestsRunAtOnce() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket a=g.begin("day1",0),b=g.begin("day2",0);
  assertNotNull(a);assertNotNull(b);assertNull(g.begin("day3",0));
  g.complete(a,1,true);assertNotNull(g.begin("day3",2));
 }
 @Test public void failureHasBoundedRetryDelayAndDoesNotPoisonSuccess() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket a=g.begin("day",0);g.complete(a,10,false);
  assertNull(g.begin("day",5009));assertNotNull(g.begin("day",5010));
  g.complete(a,6000,true);assertTrue(g.busy());
 }
 @Test public void oldAccountCompletionCannotCancelNewWork() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket old=g.begin("same",0);g.clear();
  PanelGuideRefreshGate.Ticket fresh=g.begin("same",1);
  g.complete(old,2,true);assertTrue(g.current(fresh));assertTrue(g.busy());
  g.complete(fresh,3,true);assertFalse(g.busy());
 }
 @Test public void completedCacheIsBoundedWithoutEvictingActiveWork() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket running=g.begin("running",0);
  for(int i=0;i<100;i++){PanelGuideRefreshGate.Ticket t=g.begin("day"+i,i);assertNotNull(t);g.complete(t,i,true);}
  assertEquals(PanelGuideRefreshGate.MAX_KEYS,g.size());assertTrue(g.current(running));
 }
 @Test public void aCompletionCanOnlyReleaseItsSlotOnce() {
  PanelGuideRefreshGate g=new PanelGuideRefreshGate();
  PanelGuideRefreshGate.Ticket a=g.begin("a",0);g.complete(a,1,true);g.complete(a,2,true);
  assertNotNull(g.begin("b",3));assertNotNull(g.begin("c",3));assertNull(g.begin("d",3));
 }
}
