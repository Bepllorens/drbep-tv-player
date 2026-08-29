package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TouchFilterPickerRowUiModelTest {
    @Test
    public void carriesTrimmedLogoAndFallbackMark() {
        TouchFilterPickerRowUiModel row = new TouchFilterPickerRowUiModel(
                "Deportes",
                "18 canales",
                " https://images.example/deportes.png ",
                " DEPO ",
                true,
                false,
                null
        );

        assertEquals("https://images.example/deportes.png", row.logoUrl);
        assertEquals("DEPO", row.logoText);
    }
}
