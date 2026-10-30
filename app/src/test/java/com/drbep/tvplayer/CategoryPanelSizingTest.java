package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class CategoryPanelSizingTest {
 @Test public void sampleMatchesStableFullSortWithoutChangingSource() {
  Random random=new Random(341);
  for(int pass=0;pass<100;pass++) {
   List<String> labels=new ArrayList<>();
   for(int i=0;i<1000;i++) labels.add(String.join("",Collections.nCopies(random.nextInt(120),"a")));
   List<String> original=new ArrayList<>(labels), expected=new ArrayList<>(labels);
   expected.sort((a,b)->Integer.compare(b.length(),a.length()));
   assertEquals(expected.subList(0,4),CategoryPanelSizing.longestLabels(labels));
   assertEquals(original,labels);
  }
 }
 @Test public void emptyAndNullNamesAreSafe() {
  assertTrue(CategoryPanelSizing.longestLabels(Collections.emptyList()).isEmpty());
  assertEquals(Arrays.asList("abc","x"),CategoryPanelSizing.longestLabels(Arrays.asList(null,"x","abc")));
 }
 @Test public void smallWindowsNeverOverflow() {
  for(int width=1;width<600;width++) {
   float result=CategoryPanelSizing.width(width,1000,1000);
   assertTrue(result>0 && result<=width*.90f+.01f);
  }
 }
 @Test public void tvKeepsCarouselSpaceAndRespondsToLabels() {
  assertEquals(240f,CategoryPanelSizing.width(960,100,100),.01f);
  assertEquals(312f,CategoryPanelSizing.width(960,400,100),.01f);
  assertEquals(360f,CategoryPanelSizing.width(960,1000,1000),.01f);
  assertTrue(CategoryPanelSizing.width(700,1000,1000)<=700*.38f+.01f);
 }
 @Test public void invalidDimensionsHaveBoundedFallbacks() {
  assertEquals(0f,CategoryPanelSizing.width(Float.NaN,10,10),0f);
  assertEquals(0f,CategoryPanelSizing.width(0,10,10),0f);
  assertEquals(240f,CategoryPanelSizing.width(960,Float.NaN,Float.POSITIVE_INFINITY),0f);
 }
}
