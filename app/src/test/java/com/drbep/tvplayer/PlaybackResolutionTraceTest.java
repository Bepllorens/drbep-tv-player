package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackResolutionTraceTest {
 @Test public void unstartedAndStaleAttemptsHaveNoLink(){
  PlaybackResolutionTrace t=new PlaybackResolutionTrace();assertNull(t.link(1,100));
  t.begin(1,10,100);assertArrayEquals(new long[]{10,40,1},t.link(1,140));
  t.begin(2,11,150);assertNull(t.link(1,180));
  assertArrayEquals(new long[]{11,30,2},t.link(2,180));
 }
 @Test public void controllersAndRepreparesRemainDistinct(){
  PlaybackResolutionTrace a=new PlaybackResolutionTrace(),b=new PlaybackResolutionTrace();
  a.begin(1,10,100);b.begin(1,20,120);
  assertArrayEquals(new long[]{10,60,1},a.link(1,160));
  assertArrayEquals(new long[]{20,40,1},b.link(1,160));
  assertArrayEquals(new long[]{10,100,1},a.link(1,200));
 }
 @Test public void timeBeforeStartIsNotReported(){
  PlaybackResolutionTrace t=new PlaybackResolutionTrace();t.begin(1,1,100);assertNull(t.link(1,99));
 }
}
