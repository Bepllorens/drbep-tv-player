package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

public class PlaybackDiagnosticsFormatterTest {
    @Test
    public void formatsDetailedQualityWithoutUiDependencies() {
        assertEquals(
                "1920x1080  ·  avc1.4d401f  ·  50 fps  ·  6.5 Mbps  ·  Audio mp4a",
                PlaybackDiagnosticsFormatter.detailed(diagnostics(1920, 1080, "avc1.4d401f", 6_500_000, 50f, "mp4a"), "Desconocida", Locale.US)
        );
    }

    @Test
    public void formatsCompactQualityAndNormalizesCodecs() {
        assertEquals(
                "4K  ·  H.265  ·  25 fps  ·  18.0 Mbps",
                PlaybackDiagnosticsFormatter.compact(diagnostics(3840, 2160, "hvc1.2.4", 18_000_000, 25f, ""), Locale.US)
        );
        assertEquals("H.264", PlaybackDiagnosticsFormatter.compactCodec("avc1.640028"));
    }

    @Test
    public void usesCallerFallbackWhenQualityIsMissing() {
        PlayerController.PlaybackDiagnostics empty = diagnostics(0, 0, "", 0, 0f, "");

        assertEquals("Sin datos", PlaybackDiagnosticsFormatter.detailed(empty, "Sin datos", Locale.US));
        assertEquals("", PlaybackDiagnosticsFormatter.compact(empty, Locale.US));
    }

    private static PlayerController.PlaybackDiagnostics diagnostics(
            int width,
            int height,
            String videoCodec,
            int bitrate,
            float frameRate,
            String audioCodec
    ) {
        return new PlayerController.PlaybackDiagnostics(
                "Canal", "READY", "", "", "", "", "", "", false, false, "",
                width, height, videoCodec, bitrate, frameRate, audioCodec,
                1, 1_000L, 1_500L, 0, 0L, true, true, 30_000L,
                AdaptivePlaybackQualityPolicy.LEVEL_NONE, "", "PLAYING", 4,
                true, true, "wifi", 0
        );
    }
}
