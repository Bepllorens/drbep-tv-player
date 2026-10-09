package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class U7dSeekWaitPolicyTest {
    @Test public void waitsOnlyJustAheadOfAvailableVideo() {
        long[] window = {1000, 45000};
        assertTrue(U7dSeekWaitPolicy.shouldWait(47000, window, 0));
        assertFalse(U7dSeekWaitPolicy.shouldWait(44000, window, 0));
        assertFalse(U7dSeekWaitPolicy.shouldWait(90000, window, 0));
        assertFalse(U7dSeekWaitPolicy.shouldWait(0, window, 0));
    }
    @Test public void neverWaitsIndefinitelyOrWithoutAWindow() {
        assertFalse(U7dSeekWaitPolicy.shouldWait(47000, new long[]{1000,45000}, 2));
        assertFalse(U7dSeekWaitPolicy.shouldWait(47000, null, 0));
    }
}
