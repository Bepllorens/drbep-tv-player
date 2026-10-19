package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ArchiveGridSlotsTest {
 @Test public void changedDayReusesCells(){
  ArchiveGridSlots s=new ArchiveGridSlots();
  List<Long> a=s.assign("today",Arrays.asList("a","b"));
  assertEquals(a,s.assign("yesterday",Arrays.asList("c","d")));
 }
 @Test public void refreshPreservesIdentityWhenOrderChanges(){
  ArchiveGridSlots s=new ArchiveGridSlots();
  List<Long>a=s.assign("today",Arrays.asList("a","b"));
  assertEquals(Arrays.asList(a.get(1),a.get(0)),s.assign("today",Arrays.asList("b","a")));
 }
 @Test public void newProgrammeDoesNotInheritExistingProgrammeKey(){
  ArchiveGridSlots s=new ArchiveGridSlots();
  List<Long>a=s.assign("today",Arrays.asList("a","b"));
  List<Long>b=s.assign("today",Arrays.asList("c","b"));
  assertNotEquals(a.get(0),b.get(0));assertEquals(a.get(1),b.get(1));
 }
 @Test public void emptyAndGrowingPagesHaveUniqueKeys(){
  ArchiveGridSlots s=new ArchiveGridSlots();s.assign("a",Collections.emptyList());
  List<Long>a=s.assign("b",Arrays.asList("a","b","c"));
  assertEquals(3,new HashSet<>(a).size());
 }
}
