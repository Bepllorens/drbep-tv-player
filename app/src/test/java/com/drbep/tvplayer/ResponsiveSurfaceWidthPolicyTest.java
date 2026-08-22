package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ResponsiveSurfaceWidthPolicyTest {
    @Test
    public void phoneTouchUsesAllAvailableWidth() {
        assertEquals(1560, ResponsiveSurfaceWidthPolicy.resolvePanelWidth(1600, 40, 1040, 370, true));
    }

    @Test
    public void tabletAndTvKeepBoundedPanelWidth() {
        assertEquals(1240, ResponsiveSurfaceWidthPolicy.resolvePanelWidth(2560, 0, 1240, 440, false));
    }

    @Test
    public void narrowScreensNeverFallBelowTheSafeMinimum() {
        assertEquals(370, ResponsiveSurfaceWidthPolicy.resolvePanelWidth(320, 40, 1040, 370, true));
    }
}
