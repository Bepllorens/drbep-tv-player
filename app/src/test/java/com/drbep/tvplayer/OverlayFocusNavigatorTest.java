package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OverlayFocusNavigatorTest {
    @Test
    public void wrapsForwardAtEnd() {
        assertEquals(0, OverlayFocusNavigator.nextSelection(5, 4, 2, 1));
    }

    @Test
    public void wrapsBackwardAtStart() {
        assertEquals(4, OverlayFocusNavigator.nextSelection(5, 0, 2, -1));
    }

    @Test
    public void usesCurrentIndexWhenSelectedIsInvalid() {
        assertEquals(3, OverlayFocusNavigator.nextSelection(5, -1, 2, 1));
    }

    @Test
    public void fallsBackToFirstItemWhenSelectionAndCurrentAreInvalid() {
        assertEquals(1, OverlayFocusNavigator.nextSelection(5, -1, -1, 1));
    }

    @Test
    public void keepsSelectionForEmptyLists() {
        assertEquals(3, OverlayFocusNavigator.safeSelection(0, 3, 1));
    }
}
