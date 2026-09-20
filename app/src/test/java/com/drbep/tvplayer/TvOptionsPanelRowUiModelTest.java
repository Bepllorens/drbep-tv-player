package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvOptionsPanelRowUiModelTest {
    @Test public void normalMenusKeepCompactRows() {
        assertFalse(new TvOptionsPanelRowUiModel("Opción", "1", null).wrapLabel);
    }
    @Test public void trackRowsKeepAllTechnicalDetails() {
        String label = "✓ ES-ES · Dolby Digital Plus · 5.1 · 640 kb/s · Audiodescripción · Pista 16";
        TvOptionsPanelRowUiModel row = new TvOptionsPanelRowUiModel(label, "16", null, null, true);
        assertTrue(row.wrapLabel);
        assertEquals(label, row.label);
    }
}
