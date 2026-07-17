package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Test
    public void visibleSurfaceCanAvoidStealingFocus() {
        OfflineOverlayState state = new OfflineOverlayState();
        state.setVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS, true);

        state.setVisible(OfflineOverlayState.Surface.TIMESHIFT, true, false);

        assertTrue(state.isVisible(OfflineOverlayState.Surface.TIMESHIFT));
        assertEquals(OfflineOverlayState.Surface.TOUCH_CONTROLS, state.focusedSurface());
    }

    @Test
    public void closingFocusedSurfaceFallsBackToAnotherVisibleSurface() {
        OfflineOverlayState state = new OfflineOverlayState();
        state.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, true);
        state.setVisible(OfflineOverlayState.Surface.TIMESHIFT, true);

        state.setVisible(OfflineOverlayState.Surface.TIMESHIFT, false);

        assertFalse(state.isVisible(OfflineOverlayState.Surface.TIMESHIFT));
        assertEquals(OfflineOverlayState.Surface.CHANNEL_LIST, state.focusedSurface());
    }

    @Test
    public void resetClearsVisibilityAndFocus() {
        OfflineOverlayState state = new OfflineOverlayState();
        state.setVisible(OfflineOverlayState.Surface.CHANNEL_LIST, true);
        state.setVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS, true);

        state.reset();

        assertFalse(state.isVisible(OfflineOverlayState.Surface.CHANNEL_LIST));
        assertFalse(state.isVisible(OfflineOverlayState.Surface.TOUCH_CONTROLS));
        assertNull(state.focusedSurface());
    }
}
