package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ZapBannerLayoutPolicyTest {
    @Test
    public void landscapePhoneUsesComfortableMetricsAndSingleActionRow() {
        assertFalse(ZapBannerLayoutPolicy.useCompactMetrics(700, 360));
        assertFalse(ZapBannerLayoutPolicy.stackActions(700, 360));
        assertTrue(ZapBannerLayoutPolicy.showInlineTools(true));
    }

    @Test
    public void tabletAndTvKeepSingleComfortableRow() {
        assertFalse(ZapBannerLayoutPolicy.useCompactMetrics(1280, 800));
        assertFalse(ZapBannerLayoutPolicy.stackActions(1280, 800));
        assertFalse(ZapBannerLayoutPolicy.showInlineTools(false));
    }

    @Test
    public void NarrowLargeScreenMayUseCompactMetrics() {
        assertTrue(ZapBannerLayoutPolicy.useCompactMetrics(540, 600));
        assertTrue(ZapBannerLayoutPolicy.stackActions(540, 600));
    }
}
