package com.drbep.tvplayer.netflix;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetflixFrameRateTest {
    @Test public void doesNotGuessBeforeEnoughFrames(){
        NetflixFrameRate rate=new NetflixFrameRate();
        for(int i=0;i<8;i++)rate.record(i*41667L);
        assertEquals(-1,rate.estimate(),0);
    }
    @Test public void derivesCadenceFromPresentationTimes(){
        NetflixFrameRate rate=new NetflixFrameRate();
        for(int i=0;i<40;i++)rate.record(Math.round(i*1000000.0/23.976));
        assertEquals(23.976,rate.estimate(),0.001);
    }
    @Test public void occasionalDroppedFramesDoNotHalveTheCadence(){
        NetflixFrameRate rate=new NetflixFrameRate();long pts=0;
        for(int i=0;i<40;i++){rate.record(pts);pts+=i%9==0?83334:41667;}
        assertEquals(24,rate.estimate(),0.001);
    }
    @Test public void seeksAndSourceChangesRequireFreshSamples(){
        NetflixFrameRate rate=new NetflixFrameRate();
        for(int i=0;i<20;i++)rate.record(i*40000L);
        rate.record(90000000);assertEquals(-1,rate.estimate(),0);
        for(int i=1;i<=10;i++)rate.record(90000000+i*33333L);
        assertEquals(30,rate.estimate(),0.001);
        rate.reset();assertEquals(-1,rate.estimate(),0);
        rate.record(1000);rate.record(500);assertEquals(-1,rate.estimate(),0);
    }
}
