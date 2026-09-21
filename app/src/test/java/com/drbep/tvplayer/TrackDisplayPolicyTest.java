package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class TrackDisplayPolicyTest {
    @Test public void identicalDescriptionsHaveExplicitVariantCounters() {
        java.util.List<String> labels = java.util.Arrays.asList(
                "ES-ES · AAC · Pista 8", "ES-ES · AAC · Pista 9",
                "EN-US · AAC · Pista 10", "ES-ES · AAC · Pista 11", "ES-ES · AAC · Pista 12");
        assertEquals("ES-ES · AAC · Variante 1/4", TrackDisplayPolicy.variantLabel(labels, 0));
        assertEquals("ES-ES · AAC · Variante 2/4", TrackDisplayPolicy.variantLabel(labels, 1));
        assertEquals("ES-ES · AAC · Variante 4/4", TrackDisplayPolicy.variantLabel(labels, 4));
        assertEquals(labels.get(2), TrackDisplayPolicy.variantLabel(labels, 2));
    }
    @Test public void identifiesForcedTracksEvenWhenProviderOnlyLabelsTheLanguage() {
        assertEquals("ES-ES · Forzados", TrackDisplayPolicy.subtitle("ES-ES", true));
        assertEquals("Español · Forzados", TrackDisplayPolicy.subtitle("Español · Forzados", true));
        assertEquals("ES-ES", TrackDisplayPolicy.subtitle("ES-ES", false));
    }
    @Test public void distinguishesAudioEncodingsAndChannels() {
        assertEquals("ES-ES · AAC · Estéreo · 128 kb/s · Pista 1",
                TrackDisplayPolicy.audio("ES-ES", "audio/mp4a-latm", "mp4a.40.2", 2, 128000, false, 1));
        assertTrue(TrackDisplayPolicy.audio("ES-ES", "audio/eac3", "ec-3", 6, 640000, true, 2)
                .contains("Dolby Digital Plus · 5.1 · 640 kb/s · Audiodescripción"));
        assertTrue(TrackDisplayPolicy.audio("ES-ES", "audio/eac3-joc", "ec-3", 6, -1, false, 3)
                .contains("Dolby Atmos"));
    }
    @Test public void neverFallsBackToFullSubtitles() {
        assertEquals(0, TrackDisplayPolicy.forcedPriority(false, "es-ES", "es-ES"));
        assertEquals(2, TrackDisplayPolicy.forcedPriority(true, "es-ES", "es-ES"));
        assertEquals(1, TrackDisplayPolicy.forcedPriority(true, "es", "es-ES"));
        assertEquals(0, TrackDisplayPolicy.forcedPriority(true, "es-419", "es-ES"));
        assertEquals(0, TrackDisplayPolicy.forcedPriority(true, "en", "es-ES"));
    }
}
