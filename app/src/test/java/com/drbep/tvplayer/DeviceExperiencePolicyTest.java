package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceExperiencePolicyTest {
    @Test
    public void reportsRealtimeHandoffAndCapacityWhenReady() {
        DeviceExperiencePolicy.Result result = DeviceExperiencePolicy.evaluate(
                true, true, true, false, true, 2942, 3247, 2, true
        );

        assertEquals("ok", result.level);
        assertTrue(result.handoffReady);
        assertTrue(result.remoteSummary.contains("tiempo real"));
        assertTrue(result.capacitySummary.contains("2 reproducciones"));
    }

    @Test
    public void activationIsRequiredBeforeRemoteHandoff() {
        DeviceExperiencePolicy.Result result = DeviceExperiencePolicy.evaluate(
                true, false, false, false, false, 0, 0, 4, false
        );

        assertEquals("error", result.level);
        assertFalse(result.handoffReady);
        assertTrue(result.headline.contains("activacion"));
    }

    @Test
    public void heartbeatFallbackRemainsOperational() {
        DeviceExperiencePolicy.Result result = DeviceExperiencePolicy.evaluate(
                true, true, true, false, false, 100, 20, 3, false
        );

        assertEquals("warn", result.level);
        assertTrue(result.handoffReady);
        assertTrue(result.remoteSummary.contains("heartbeat"));
    }

    @Test
    public void expiredCatalogIsVisibleWithoutHidingCapabilities() {
        DeviceExperiencePolicy.Result result = DeviceExperiencePolicy.evaluate(
                true, true, true, true, true, 100, 20, 2, false
        );

        assertEquals("warn", result.level);
        assertTrue(result.handoffReady);
        assertTrue(result.catalogSummary.contains("actualizar"));
    }
}
