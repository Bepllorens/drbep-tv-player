package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaybackFinishEstimateTest {
 @Test public void estimateUsesRemainingMediaTimeAndActualSpeed(){
  long now=1_790_000_000_000L;
  assertEquals(now+3_600_000L,PlaybackFinishEstimate.calculate(now,1_800_000,5_400_000,1,true,false));
  assertEquals(now+1_800_000L,PlaybackFinishEstimate.calculate(now,1_800_000,5_400_000,2,true,false));
  assertEquals(now+7_200_000L,PlaybackFinishEstimate.calculate(now,1_800_000,5_400_000,.5,true,false));
 }
 @Test public void pauseLiveUnknownAndCompletedHaveNoFinishTime(){
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,10,100,1,false,false));
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,10,100,1,true,true));
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,10,-1,1,true,false));
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,100,100,1,true,false));
  for(double speed:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})
   assertEquals(0,PlaybackFinishEstimate.calculate(1000,10,100,speed,true,false));
 }
 @Test public void resumeMovesFinishByPausedWallTimeAndFractionalSpeedRoundsUp(){
  long first=PlaybackFinishEstimate.calculate(10000,10,1010,1,true,false);
  long resumed=PlaybackFinishEstimate.calculate(40000,10,1010,1,true,false);
  assertEquals(30000,resumed-first);
  assertEquals(1334,PlaybackFinishEstimate.calculate(1000,0,1001,3,true,false));
 }
 @Test public void rejectsBogusDurationsAndOverflow(){
  assertEquals(0,PlaybackFinishEstimate.calculate(Long.MAX_VALUE-5,0,100,1,true,false));
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,0,Long.MAX_VALUE,1,true,false));
  assertEquals(0,PlaybackFinishEstimate.calculate(1000,0,60000,Double.MIN_VALUE,true,false));
 }
 @Test public void actualDurationWinsWithoutOverflowingMetadata(){
  assertEquals(5000,PlaybackFinishEstimate.duration(5000,99));
  assertEquals(99000,PlaybackFinishEstimate.duration(-1,99));
  assertEquals(0,PlaybackFinishEstimate.duration(0,Long.MAX_VALUE));
 }
}
