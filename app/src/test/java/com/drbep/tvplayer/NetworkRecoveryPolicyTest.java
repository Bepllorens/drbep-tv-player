package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NetworkRecoveryPolicyTest {
    @Test
    public void usesBoundedBackoff() {
        assertEquals(750L, NetworkRecoveryPolicy.retryDelayMs(0));
        assertEquals(1_500L, NetworkRecoveryPolicy.retryDelayMs(1));
        assertEquals(5_000L, NetworkRecoveryPolicy.retryDelayMs(20));
    }

    @Test
    public void retriesOnlyWhenPlaybackStillNeedsRecovery() {
        assertTrue(NetworkRecoveryPolicy.shouldRetryAfterRestore(true, false, "waiting_network"));
        assertTrue(NetworkRecoveryPolicy.shouldRetryAfterRestore(true, false, "error"));
        assertFalse(NetworkRecoveryPolicy.shouldRetryAfterRestore(true, true, "playing"));
        assertFalse(NetworkRecoveryPolicy.shouldRetryAfterRestore(false, false, "waiting_network"));
    }
}
