package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerControllerDashManifestRecoveryTest {
    private static final class DashManifestStaleException extends Exception {
        DashManifestStaleException() {
            super("stale");
        }
    }

    @Test
    public void rejectsUnrelatedErrors() {
        assertFalse(PlayerController.isDashManifestStale(new IllegalStateException("network")));
    }

    @Test
    public void detectsStaleManifestInCauseChain() {
        Throwable wrapped = new IllegalStateException(new DashManifestStaleException());
        assertTrue(PlayerController.isDashManifestStale(wrapped));
    }
}
