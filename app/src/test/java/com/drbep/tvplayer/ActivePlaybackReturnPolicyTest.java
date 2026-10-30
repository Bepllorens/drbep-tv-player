package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class ActivePlaybackReturnPolicyTest {
 @Test public void sameActiveItemCanReturn(){assertTrue(ActivePlaybackReturnPolicy.allows("item","item",true,true));}
 @Test public void changedOrStoppedPlaybackCannotReturn(){
  assertFalse(ActivePlaybackReturnPolicy.allows("item","other",true,true));
  assertFalse(ActivePlaybackReturnPolicy.allows("item","item",false,true));
 }
 @Test public void missingIdentityOrPermissionCannotReturn(){
  assertFalse(ActivePlaybackReturnPolicy.allows("","",true,true));
  assertFalse(ActivePlaybackReturnPolicy.allows(null,null,true,true));
  assertFalse(ActivePlaybackReturnPolicy.allows("item","item",true,false));
 }
}
