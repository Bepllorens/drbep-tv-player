package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackDockGeometryTest {
 @Test public void focusInsideViewportDoesNotMoveRow(){
  assertEquals(0,PlaybackDockGeometry.scrollTarget(2,14,48,8,320,0));
  assertEquals(100,PlaybackDockGeometry.scrollTarget(3,14,48,8,320,100));
 }
 @Test public void focusedCellIsFullyVisibleAcrossNarrowAndWideScreens(){
  for(int count=1;count<=40;count++)for(int width:new int[]{240,320,600,805,1280}){
   int scroll=0,content=count*48+(count-1)*8,max=Math.max(0,content-width);
   for(int round=0;round<2;round++)for(int step=0;step<count;step++){
    int selected=round==0?step:count-1-step;
    scroll=PlaybackDockGeometry.scrollTarget(selected,count,48,8,width,scroll);
    assertTrue(scroll>=0 && scroll<=max);
    assertTrue(selected*56>=scroll);
    assertTrue(selected*56+48<=scroll+width);
   }
  }
 }
 @Test public void fittingRowHasNoScrollAndBoundsAreSafe(){
  assertEquals(0,PlaybackDockGeometry.scrollTarget(7,8,48,8,805,100));
  assertEquals(0,PlaybackDockGeometry.scrollTarget(0,0,48,8,320,0));
  assertEquals(0,PlaybackDockGeometry.scrollTarget(-1,14,48,8,320,500));
  assertTrue(PlaybackDockGeometry.scrollTarget(Integer.MAX_VALUE,Integer.MAX_VALUE,48,8,320,0)>0);
 }
}
