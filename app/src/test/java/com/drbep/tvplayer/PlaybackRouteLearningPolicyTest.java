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
}
