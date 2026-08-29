package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaybackHealthClassifierTest {
    @Test
    public void classifiesReadyPlaybackAsOk() {
        PlaybackHealthClassifier.Result result = PlaybackHealthClassifier.classify(diagnostics("READY", "", true, 1, 500), 60_000L);

        assertEquals("ok", result.level);
        assertEquals("Reproduccion estable", result.summary);
        assertEquals(0.008d, result.rebufferRatio, 0.0001d);
    }

    @Test
    public void classifiesErrorAsError() {
        PlaybackHealthClassifier.Result result = PlaybackHealthClassifier.classify(diagnostics("READY", "Source error", true, 0, 0), 10_000L);

        assertEquals("error", result.level);
        assertEquals("Error de reproduccion", result.summary);
    }

    @Test
    public void classifiesReadyWithoutFirstFrameAsWarning() {
        PlaybackHealthClassifier.Result result = PlaybackHealthClassifier.classify(diagnostics("READY", "", false, 0, 0), 10_000L);

        assertEquals("warning", result.level);
        assertEquals("Listo sin primer frame", result.summary);
    }

    @Test
    public void classifiesHighBufferingAsWarning() {
        PlaybackHealthClassifier.Result result = PlaybackHealthClassifier.classify(diagnostics("READY", "", true, 5, 7_000), 60_000L);

        assertEquals("warning", result.level);
        assertEquals("Buffer inestable", result.summary);
    }

    private static PlayerController.PlaybackDiagnostics diagnostics(String state, String error, boolean firstFrame, int bufferingCount, long bufferingMs) {
        return new PlayerController.PlaybackDiagnostics(
                "Canal",
                state,
                "",
                "",
                "",
                "",
                "",
                "",
                false,
                false,
                error,
                1920,
                1080,
                "avc1",
                5_000_000,
                25f,
                "mp4a",
                1,
                1000L,
                1500L,
                bufferingCount,
                bufferingMs,
                firstFrame,
                true,
                30_000L,
                AdaptivePlaybackQualityPolicy.LEVEL_NONE,
                ""
        );
    }
}
