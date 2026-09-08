package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class U7dBufferedSeekPolicyTest {
    @Test public void allowsExistingRangeOnlyWithEdgesProtected() {
        assertTrue(U7dBufferedSeekPolicy.contains(30000,0,60000));
        assertFalse(U7dBufferedSeekPolicy.contains(60000,0,60000));
        assertFalse(U7dBufferedSeekPolicy.contains(-1,0,60000));
        assertFalse(U7dBufferedSeekPolicy.contains(1000,0,0));
    }
    @Test public void slidingWindowKeepsAbsoluteCoordinates() {
        assertTrue(U7dBufferedSeekPolicy.contains(150000,120000,60000));
        assertFalse(U7dBufferedSeekPolicy.contains(30000,120000,60000));
        assertFalse(U7dBufferedSeekPolicy.contains(180000,120000,60000));
    }
}
