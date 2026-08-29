package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OfflineTvThemeTest {
    @Test
    public void unknownPaletteFallsBackToAurora() {
        assertEquals(OfflineTvTheme.PALETTE_AURORA, OfflineTvTheme.normalizePaletteId("unknown"));
        assertEquals(OfflineTvTheme.PALETTE_AURORA, OfflineTvTheme.normalizePaletteId(null));
    }

    @Test
    public void supportedPaletteIdsAreNormalized() {
        assertEquals(OfflineTvTheme.PALETTE_GRAPHITE, OfflineTvTheme.normalizePaletteId(" Graphite "));
        assertEquals(OfflineTvTheme.PALETTE_EMERALD, OfflineTvTheme.normalizePaletteId("EMERALD"));
        assertEquals(OfflineTvTheme.PALETTE_HIGH_CONTRAST, OfflineTvTheme.normalizePaletteId("high-contrast"));
    }
}
