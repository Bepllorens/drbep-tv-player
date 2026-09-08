package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackGuidancePolicyTest {
    @Test
    public void reportsStablePlaybackWithoutSuggestingRecovery() {
        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("READY", "PLAYING", "", true, true, true, 0, 0L, 0, false),
                null
        );

        assertEquals("ok", result.level);
        assertEquals("Reproduccion estable", result.headline);
        assertEquals(PlaybackGuidancePolicy.ACTION_CLOSE, result.primaryAction);
    }

    @Test
    public void prioritizesNetworkRecoveryWhenDeviceIsOffline() {
        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("BUFFERING", "WAITING_NETWORK", "", false, false, false, 1, 500L, 0, false),
                null
        );

        assertEquals("error", result.level);
        assertTrue(result.headline.contains("sin conexion"));
        assertEquals(PlaybackGuidancePolicy.ACTION_RETRY, result.primaryAction);
    }

    @Test
    public void explainsAuthorizationFailures() {
        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("IDLE", "ERROR", "HTTP 403 Forbidden", true, true, false, 0, 0L, 0, false),
                null
        );

        assertEquals("error", result.level);
        assertTrue(result.headline.contains("sesion o permisos"));
        assertTrue(result.nextStep.contains("catalogo"));
        assertEquals(PlaybackGuidancePolicy.ACTION_RETRY, result.primaryAction);
    }

    @Test
    public void suggestsAnotherRouteForManifestFailures() {
        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("IDLE", "ERROR", "response does not look like SmoothStreaming manifest", true, true, false, 0, 0L, 0, false),
                null
        );

        assertTrue(result.headline.contains("manifiesto"));
        assertEquals(PlaybackGuidancePolicy.ACTION_NEXT_ROUTE, result.primaryAction);
    }

    @Test
    public void describesRepeatedBufferingAsUnstable() {
        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("READY", "PLAYING", "", true, true, true, 5, 7_000L, 2, false),
                null
        );

        assertEquals("warn", result.level);
        assertTrue(result.headline.contains("inestable"));
        assertEquals(PlaybackGuidancePolicy.ACTION_NEXT_ROUTE, result.primaryAction);
    }

    @Test
    public void doesNotPresentHistoricalErrorAsCurrentFailure() {
        PlaybackDiagnosticsStore.ErrorRecord stored = new PlaybackDiagnosticsStore.ErrorRecord(
                "1", "Canal", "Source error", "Directo", "auto", 1L
        );

        PlaybackGuidancePolicy.Result result = PlaybackGuidancePolicy.evaluate(
                diagnostics("READY", "PLAYING", "", true, true, true, 0, 0L, 0, false),
                stored
        );

        assertEquals("warn", result.level);
        assertEquals("El canal funciona ahora", result.headline);
        assertEquals(PlaybackGuidancePolicy.ACTION_CLOSE, result.primaryAction);
    }

    private static PlayerController.PlaybackDiagnostics diagnostics(
            String state,
            String phase,
            String error,
            boolean networkAvailable,
            boolean networkValidated,
            boolean firstFrame,
            int bufferingCount,
            long bufferingMs,
            int recoveryAttempts,
            boolean fallback
    ) {
        return new PlayerController.PlaybackDiagnostics(
                "Canal",
                state,
                phase,
                "Directo HLS",
                "https://example.invalid/stream",
                "application/x-mpegURL",
                "none",
                "auto",
                false,
                fallback,
                error,
                1920,
                1080,
                "avc1",
                5_000_000,
                25f,
                "mp4a",
                1,
                1_000L,
                1_500L,
                bufferingCount,
                bufferingMs,
                firstFrame,
                firstFrame,
                30_000L,
                AdaptivePlaybackQualityPolicy.LEVEL_NONE,
                "",
                firstFrame ? "PLAYING" : "PREPARING",
                4,
                networkAvailable,
                networkValidated,
                "wifi",
                recoveryAttempts
        );
    }
}
