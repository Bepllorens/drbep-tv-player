package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class LiveDvrAnchorTest {
 @Test public void rollingWindowPreservesProgrammeNotRelativePosition(){
  LiveDvrAnchor anchor=new LiveDvrAnchor("a",1000000,300000,600000,1000,false);
  LiveDvrAnchor.Resolution r=anchor.resolve("a",1060000,600000,61000);
  assertEquals(240000,r.positionMs);assertFalse(r.expired);assertFalse(r.paused);
 }
 @Test public void fallbackSubtractsOutageFromDelay(){
  LiveDvrAnchor anchor=new LiveDvrAnchor("a",-1,300000,600000,1000,false);
  assertEquals(240000,anchor.resolve("a",-1,600000,61000).positionMs);
 }
 @Test public void expiredPointClampsAndKeepsPause(){
  LiveDvrAnchor anchor=new LiveDvrAnchor("a",1000000,10000,600000,1000,true);
  LiveDvrAnchor.Resolution r=anchor.resolve("a",1020000,600000,21000);
  assertEquals(0,r.positionMs);assertTrue(r.expired);assertTrue(r.paused);
 }
 @Test public void cannotRestoreDifferentChannelOrEmptyWindow(){
  LiveDvrAnchor anchor=new LiveDvrAnchor("a",1000000,10000,600000,1000,true);
  assertNull(anchor.resolve("b",1000000,600000,2000));assertNull(anchor.resolve("a",1000000,0,2000));
 }
 @Test public void livePlaybackDoesNotBecomeDelayedButPauseAtLiveIsPreserved(){
  assertFalse(new LiveDvrAnchor("a",-1,595000,600000,1000,false).shouldRestore());
  assertTrue(new LiveDvrAnchor("a",-1,595000,600000,1000,true).shouldRestore());
 }
}
