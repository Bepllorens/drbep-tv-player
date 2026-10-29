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
}
