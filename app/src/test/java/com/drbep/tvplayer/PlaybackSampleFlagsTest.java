package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackSampleFlagsTest {
 @Test public void allCombinationsRoundTrip(){
  for(int state=1;state<=4;state++)for(int bits=0;bits<16;bits++){
   int value=PlaybackSampleFlags.encode(state,(bits&1)!=0,(bits&2)!=0,(bits&4)!=0,(bits&8)!=0);
   assertEquals(state,value&7);assertEquals(bits,value>>3);
  }
 }
 @Test public void readyPausedIsNotReportedPlaying(){
  assertEquals(3,PlaybackSampleFlags.encode(3,false,false,false,false));
 }
 @Test public void bufferingCanBeRequestedWithoutAdvancing(){
  assertEquals(42,PlaybackSampleFlags.encode(2,true,false,true,false));
 }
 @Test public void suppressionIsIndependentOfPause(){
  assertEquals(75,PlaybackSampleFlags.encode(3,true,false,false,true));
 }
}
