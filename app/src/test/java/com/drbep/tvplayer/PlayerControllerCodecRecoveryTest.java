package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerControllerCodecRecoveryTest {
    @Test
    public void detectsVideoRendererFailure() {
        assertTrue(PlayerController.isMediaCodecRendererError(
                new IllegalStateException("MediaCodecVideoRenderer error, format_supported=YES")
        ));
    }

    @Test
    public void detectsNestedAudioCodecFailure() {
        RuntimeException codec = new RuntimeException("Media codec failed while queuing input");
        assertTrue(PlayerController.isMediaCodecRendererError(
                new IllegalStateException("Playback failed", codec)
        ));
    }

    @Test
    public void doesNotTreatNetworkFailureAsCodecFailure() {
        assertFalse(PlayerController.isMediaCodecRendererError(
                new IllegalStateException("HTTP 502 while loading manifest")
        ));
    }

    @Test
    public void diagnosticSummaryPreservesNestedCause() {
        RuntimeException codec = new RuntimeException("codec diagnostic detail");
        String summary = PlayerController.describePlaybackError(
                new IllegalStateException("renderer failed", codec)
        );

        assertTrue(summary.contains("renderer failed"));
        assertTrue(summary.contains("codec diagnostic detail"));
        assertTrue(summary.contains(" <- "));
    }
}
