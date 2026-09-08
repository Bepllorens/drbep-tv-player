package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VodPanelActionUiModelTest {
    @Test
    public void timedActionCountsDownAndBecomesPlayableAtAvailabilityTime() {
        VodPanelActionUiModel action = new VodPanelActionUiModel(
                "Ver",
                true,
                () -> { },
                1_010_000L,
                "🕒 Empieza en",
                "Ver"
        );

        assertFalse(action.isEnabledAt(1_000_000L));
        assertEquals("🕒 Empieza en 00:00:10", action.labelAt(1_000_000L));
        assertTrue(action.isEnabledAt(1_010_000L));
        assertEquals("Ver", action.labelAt(1_010_000L));
    }

    @Test
    public void actionKeepsItsVisualTone() {
        VodPanelActionUiModel action = new VodPanelActionUiModel(
                "▶ Únete al directo",
                true,
                () -> { },
                "live"
        );

        assertEquals("live", action.tone);
    }
}
