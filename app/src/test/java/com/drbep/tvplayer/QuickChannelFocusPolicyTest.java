package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class QuickChannelFocusPolicyTest {
    private static final Runnable ACTION = () -> {
    };

    @Test
    public void firstRowUpTargetsHighlightedSearch() {
        List<ZapActionItem> actions = Arrays.asList(
                action("Anterior", false, false),
                action("Siguiente", true, false),
                action("Buscar", true, true)
        );

        assertEquals(2, QuickChannelFocusPolicy.topActionIndex(actions));
    }

    @Test
    public void lastRowDownTargetsNextPage() {
        List<ZapActionItem> actions = Arrays.asList(
                action("Anterior", false, false),
                action("Siguiente", true, false),
                action("Buscar", true, true)
        );

        assertEquals(1, QuickChannelFocusPolicy.bottomActionIndex(actions));
    }

    @Test
    public void lastPageFallsBackToPreviousPage() {
        List<ZapActionItem> actions = Arrays.asList(
                action("Anterior", true, false),
                action("Siguiente", false, false),
                action("Buscar", true, true)
        );

        assertEquals(0, QuickChannelFocusPolicy.bottomActionIndex(actions));
    }

    private static ZapActionItem action(String label, boolean enabled, boolean highlighted) {
        return new ZapActionItem(label, enabled, highlighted, false, enabled ? ACTION : null);
    }
}
