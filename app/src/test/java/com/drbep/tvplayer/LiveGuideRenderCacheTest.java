package com.drbep.tvplayer;
import org.junit.Test;import static org.junit.Assert.*;
public class LiveGuideRenderCacheTest{
 @Test public void focusReusesButDataAccountDayClockAndCloseInvalidate(){
  LiveGuideRenderCache<Object> c=new LiveGuideRenderCache<>();Object rows=new Object();
  Object first=c.get("user/channel/day",rows,1,Object::new);
  for(int i=0;i<100;i++)assertSame(first,c.get("user/channel/day",rows,1,Object::new));
  Object changed=c.get("user/channel/day",new Object(),1,Object::new);assertNotSame(first,changed);
  Object other=c.get("other/channel/day",rows,1,Object::new);assertNotSame(changed,other);
  Object next=c.get("other/channel/day",rows,2,Object::new);assertNotSame(other,next);
  c.clear();assertNotSame(next,c.get("other/channel/day",rows,2,Object::new));
 }

 @Test public void reportsHitOnlyForTheSameWindowSourceAndClock(){
  LiveGuideRenderCache<String> c=new LiveGuideRenderCache<>();Object source=new Object();
  c.get("user/channel/day",source,1,()->"one");assertFalse(c.wasHit());
  c.get("user/channel/day",source,1,()->"wrong");assertTrue(c.wasHit());
  c.get("user/channel/next-day",source,1,()->"two");assertFalse(c.wasHit());
  c.clear();assertFalse(c.wasHit());
 }
 @Test public void cachedRowsAreEquivalentAcrossFocusMovesWithoutMutatingTemplates(){
  LiveGuideRenderCache<java.util.List<String>> c=new LiveGuideRenderCache<>();
  Object source=new Object();java.util.List<String> expected=new java.util.ArrayList<>();
  for(int i=0;i<240;i++)expected.add("day|time|title"+i+"|description|progress");
  for(int selected=0;selected<240;selected++){
   java.util.List<String> cached=c.get("scope/channel/day",source,1,()->new java.util.ArrayList<>(expected));
   assertEquals(expected,cached);
   java.util.List<String> view=new java.util.ArrayList<>(cached);
   view.set(selected,view.get(selected)+"|selected");
   assertEquals(expected,cached);
   assertEquals(selected>0,c.wasHit());
  }
 }
 @Test public void refreshAndCloseRebuildEvenWithEquivalentData(){
  LiveGuideRenderCache<String> c=new LiveGuideRenderCache<>();
  Object first=new Object();java.util.concurrent.atomic.AtomicInteger builds=new java.util.concurrent.atomic.AtomicInteger();
  java.util.function.Supplier<String> build=()->{builds.incrementAndGet();return "equivalent";};
  assertEquals("equivalent",c.get("window",first,1,build));
  assertEquals("equivalent",c.get("window",new Object(),1,build));assertFalse(c.wasHit());
  assertEquals("equivalent",c.get("window",first,2,build));assertFalse(c.wasHit());
  c.clear();assertEquals("equivalent",c.get("window",first,2,build));assertFalse(c.wasHit());
  assertEquals(4,builds.get());
 }
}
