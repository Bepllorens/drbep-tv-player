package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OfflineOverlayStateTest {
    @Test
    public void visibleSurfaceBecomesFocused() {
        OfflineOverlayState state = new OfflineOverlayState();

        state.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, true);

        assertTrue(state.isVisible(OfflineOverlayState.Surface.CHANNEL_LIST));
        assertEquals(OfflineOverlayState.Surface.CHANNEL_LIST, state.focusedSurface());
    }

    @Test
    public void blockingPanelIgnoresTransientHud() {
        OfflineOverlayState state = new OfflineOverlayState();

        state.setVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS, true);
        assertFalse(state.hasBlockingPanelVisible());

        state.setVisible(OfflineOverlayState.Surface.RECORDINGS, true);
        assertTrue(state.hasBlockingPanelVisible());
    }

    @Test
    public void clearingTransientSurfacesKeepsBlockingPanels() {
        OfflineOverlayState state = new OfflineOverlayState();
        state.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, true);
        state.setVisible(OfflineOverlayState.Surface.TIMESHIFT, true);
        state.setVisible(OfflineOverlayState.Surface.ZAP_BANNER, true);

        state.clearTransientPlaybackSurfaces();

        assertTrue(state.isVisible(OfflineOverlayState.Surface.CHANNEL_LIST));
        assertFalse(state.isVisible(OfflineOverlayState.Surface.TIMESHIFT));
        assertFalse(state.isVisible(OfflineOverlayState.Surface.ZAP_BANNER));
    }
}
