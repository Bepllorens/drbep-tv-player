package com.drbep.tvplayer;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class LiveGuideSelectionTest {
 @Test public void remembersByIdentityAcrossDaysAndReordering(){
  LiveGuideSelection s=new LiveGuideSelection();List<String> ids=Arrays.asList("a","b","c");
  assertEquals(1,s.resolve("channel|today",ids,1));
  assertEquals(2,s.move("channel|today",ids,1,1));
  assertEquals(0,s.resolve("channel|tomorrow",ids,0));
  assertEquals(0,s.resolve("channel|today",Arrays.asList("c","b","a"),1));
  assertEquals(0,s.resolve("other|today",ids,0));
 }
 @Test public void clampsAndRecoversMissingEvents(){
  LiveGuideSelection s=new LiveGuideSelection();assertEquals(-1,s.resolve("w",Collections.emptyList(),0));
  assertEquals(0,s.move("w",Arrays.asList("a","b"),0,-1));
  assertEquals(1,s.move("w",Arrays.asList("a","b"),0,9));
  assertEquals(0,s.resolve("w",Collections.singletonList("a"),99));
 }
 @Test public void boundsMemory(){
  LiveGuideSelection s=new LiveGuideSelection();List<String> ids=Arrays.asList("a","b");
  s.select("old","b");for(int i=0;i<12;i++)s.select("new"+i,"b");
  assertEquals(0,s.resolve("old",ids,0));
 }
}
