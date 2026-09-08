package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class U7dReplayRecoveryPolicyTest {
    @Test public void retainsPositionBehindAdvancingWindow() {
        assertEquals(90000L, U7dReplayRecoveryPolicy.windowPosition(120000, -30000));
    }
    @Test public void reconstructsNormalWindowPosition() {
        assertEquals(150000L, U7dReplayRecoveryPolicy.windowPosition(120000, 30000));
        assertEquals(30000L, U7dReplayRecoveryPolicy.windowPosition(0, 30000));
    }
    @Test public void boundsReconstructedWindowPosition() {
        assertEquals(0L, U7dReplayRecoveryPolicy.windowPosition(120000, -150000));
        assertEquals(Long.MAX_VALUE, U7dReplayRecoveryPolicy.windowPosition(Long.MAX_VALUE, 1));
    }
    @Test public void retainsPositionRatherThanRestartingAtZero() {
        assertEquals(95000L, U7dReplayRecoveryPolicy.absolutePosition(0, 95000, 3600000));
    }
    @Test public void includesOffsetFromPreviousReplay() {
        assertEquals(695000L, U7dReplayRecoveryPolicy.absolutePosition(600000, 95000, 3600000));
    }
    @Test public void clampsInvalidAndEndCoordinates() {
        assertEquals(0L, U7dReplayRecoveryPolicy.absolutePosition(-1, -1, 3600000));
        assertEquals(3599000L, U7dReplayRecoveryPolicy.absolutePosition(Long.MAX_VALUE, Long.MAX_VALUE, 3600000));
        assertEquals(0L, U7dReplayRecoveryPolicy.absolutePosition(1000, 2000, 0));
    }
}
