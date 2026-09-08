package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class OverlayScrollVisibilityTest {
    @Test public void visibleIncludingExactEdges() {
        assertTrue(OverlayScrollVisibility.isFullyVisible(0, 60, 0, 300));
        assertTrue(OverlayScrollVisibility.isFullyVisible(240, 60, 0, 300));
    }
    @Test public void clippedOrUnmeasuredRowsNeedReveal() {
        assertFalse(OverlayScrollVisibility.isFullyVisible(-1, 60, 0, 300));
        assertFalse(OverlayScrollVisibility.isFullyVisible(241, 60, 0, 300));
        assertFalse(OverlayScrollVisibility.isFullyVisible(0, 0, 0, 300));
        assertFalse(OverlayScrollVisibility.isFullyVisible(0, 60, 0, 0));
        assertFalse(OverlayScrollVisibility.isFullyVisible(0, 400, 0, 300));
    }
    @Test public void supportsPaddingAndAvoidsOverflow() {
        assertTrue(OverlayScrollVisibility.isFullyVisible(-10, 60, -10, 300));
        assertFalse(OverlayScrollVisibility.isFullyVisible(Integer.MAX_VALUE, 60, 0, 300));
    }
}
