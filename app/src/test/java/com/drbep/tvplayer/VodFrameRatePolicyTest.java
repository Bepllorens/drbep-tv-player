package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class VodFrameRatePolicyTest {
    @Test public void fractionalAndIntegerAreDistinct() {
        assertFalse(VodFrameRatePolicy.matches(23.976f, 24));
        assertTrue(VodFrameRatePolicy.matches(23.976f, 47.952f));
        assertFalse(VodFrameRatePolicy.matches(29.97f, 60));
    }
    @Test public void selectsExactOrMultipleWithoutUnneededSwitch() {
        assertEquals(1, VodFrameRatePolicy.choose(23.976f, 60, new float[]{24,23.976f,59.94f}));
        assertEquals(0, VodFrameRatePolicy.choose(25, 60, new float[]{50,60}));
        assertEquals(-1, VodFrameRatePolicy.choose(24, 120, new float[]{24,60,120}));
        assertEquals(-1, VodFrameRatePolicy.choose(24, 60, new float[]{50,60}));
    }
    @Test public void invalidRatesNeverChangeDisplay() {
        for(float fps:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY})
            assertEquals(-1,VodFrameRatePolicy.choose(fps,60,new float[]{24,50,60}));
    }
}
