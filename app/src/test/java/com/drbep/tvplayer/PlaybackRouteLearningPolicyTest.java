package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackRouteLearningPolicyTest {
    @Test
    public void compatibilityFallbackWinsOverRequestedDirectMode() {
        assertEquals(
                PlaybackModeStore.MODE_COMPAT,
                PlaybackRouteLearningPolicy.effectiveMode(
                        PlaybackModeStore.MODE_DIRECT,
                        "Proxy DASH",
                        true
                )
        );
    }

    @Test
    public void routeMustRenderAndRemainErrorFreeInSameGeneration() {
        assertTrue(PlaybackRouteLearningPolicy.isStable("READY", true, true, "", 0, 10_000L, 10_500L, 7, 7));
        assertTrue(PlaybackRouteLearningPolicy.isStable("READY", true, false, "", 0, 10_000L, 40_000L, 7, 7));
        assertFalse(PlaybackRouteLearningPolicy.isStable("READY", false, true, "", 0, 10_000L, 40_000L, 7, 7));
        assertFalse(PlaybackRouteLearningPolicy.isStable("READY", true, false, "", 0, 10_000L, 20_000L, 7, 7));
        assertFalse(PlaybackRouteLearningPolicy.isStable("READY", true, true, "", 1, 10_000L, 40_000L, 7, 7));
        assertFalse(PlaybackRouteLearningPolicy.isStable("READY", true, true, "timeout", 0, 10_000L, 40_000L, 7, 7));
        assertFalse(PlaybackRouteLearningPolicy.isStable("READY", true, true, "", 0, 10_000L, 40_000L, 7, 8));
    }

    @Test
    public void invalidatesOnlyAnAutomaticLearnedRouteThatActuallyFailed() {
        assertTrue(PlaybackRouteLearningPolicy.shouldInvalidateAfterFailure(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_DIRECT
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterFailure(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_DIRECT,
                false,
                PlaybackModeStore.MODE_DIRECT
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterFailure(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_AUTO,
                true,
                PlaybackModeStore.MODE_DIRECT
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterFailure(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_PROXY
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterFailure(
                PlaybackModeStore.MODE_AUTO,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_AUTO
        ));
    }

    @Test
    public void recoveryInvalidatesTheOldLearnedRouteOnlyWhenTheModeChanged() {
        assertTrue(PlaybackRouteLearningPolicy.shouldInvalidateAfterRecovery(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_PROXY,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_DIRECT
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterRecovery(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_DIRECT
        ));
        assertFalse(PlaybackRouteLearningPolicy.shouldInvalidateAfterRecovery(
                PlaybackModeStore.MODE_DIRECT,
                PlaybackModeStore.MODE_AUTO,
                PlaybackModeStore.MODE_AUTO,
                false,
                PlaybackModeStore.MODE_DIRECT
        ));
    }
}
