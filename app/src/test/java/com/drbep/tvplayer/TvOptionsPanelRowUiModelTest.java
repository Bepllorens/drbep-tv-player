package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TvOptionsPanelRowUiModelTest {
    @Test public void existingMenusKeepNumericFallbackAndAction() {
        Runnable action = () -> {};
        TvOptionsPanelRowUiModel row = new TvOptionsPanelRowUiModel("Canal", "3", action);
        assertEquals("3", row.indexLabel);
        assertSame(action, row.onClick);
        assertNull(row.bindArtwork);
    }
    @Test public void optionalArtworkDoesNotReplaceSelectionAction() {
        Runnable action = () -> {};
        java.util.function.Consumer<android.widget.ImageView> artwork = view -> {};
        TvOptionsPanelRowUiModel row = new TvOptionsPanelRowUiModel(null, null, action, artwork);
        assertEquals("", row.label);
        assertEquals("", row.indexLabel);
        assertSame(action, row.onClick);
        assertSame(artwork, row.bindArtwork);
    }
}
