package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
public class LiveGuideActionPolicyTest {
 @Test public void datesDoNotGrantPlayback(){
  long now=1000000000L;
  assertFalse(LiveGuideActionPolicy.replay(now-1000,now+1000,now,true,false));
  assertFalse(LiveGuideActionPolicy.replay(now-1000,now+1000,now,false,true));
  assertTrue(LiveGuideActionPolicy.replay(now-1000,now+1000,now,true,true));
  assertFalse(LiveGuideActionPolicy.replay(now+1000,now+2000,now,true,true));
  assertFalse(LiveGuideActionPolicy.replay(1,2,now,true,true));
  assertFalse(LiveGuideActionPolicy.replay(3,2,now,true,true));
 }
 @Test public void expiryBoundary(){
  long end=1000000000L;
  assertEquals(LiveGuideActionPolicy.Phase.PAST,LiveGuideActionPolicy.phase(end-1000,end,end+7L*86400000-1));
  assertEquals(LiveGuideActionPolicy.Phase.EXPIRED,LiveGuideActionPolicy.phase(end-1000,end,end+7L*86400000));
 }
}
