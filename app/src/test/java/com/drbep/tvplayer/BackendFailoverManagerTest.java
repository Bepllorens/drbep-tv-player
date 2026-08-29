package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BackendFailoverManagerTest {
    @Test
    public void keepsPrimaryWhenAnyPrimaryProbeSucceeds() {
        AtomicInteger primaryCalls = new AtomicInteger();
        BackendFailoverManager.Decision decision = BackendFailoverManager.evaluate(
                "https://fire.tvbep.com/",
                "https://direct.tvbep.com/",
                baseUrl -> baseUrl.contains("fire") && primaryCalls.incrementAndGet() == 2,
                delayMs -> { }
        );

        assertFalse(decision.useEmergency);
        assertEquals("https://fire.tvbep.com", decision.selectedBaseUrl);
        assertEquals(1, decision.primaryFailures);
        assertEquals(2, primaryCalls.get());
    }

    @Test
    public void switchesOnlyAfterThreePrimaryFailuresAndHealthyEmergency() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger emergencyCalls = new AtomicInteger();
        BackendFailoverManager.Decision decision = BackendFailoverManager.evaluate(
                "https://fire.tvbep.com",
                "https://direct.tvbep.com",
                baseUrl -> {
                    if (baseUrl.contains("fire")) {
                        primaryCalls.incrementAndGet();
                        return false;
                    }
                    emergencyCalls.incrementAndGet();
                    return true;
                },
                delayMs -> { }
        );

        assertTrue(decision.useEmergency);
        assertTrue(decision.emergencyHealthy);
        assertEquals(BackendFailoverManager.PRIMARY_ATTEMPTS, decision.primaryFailures);
        assertEquals(BackendFailoverManager.PRIMARY_ATTEMPTS, primaryCalls.get());
        assertEquals(1, emergencyCalls.get());
        assertEquals("https://direct.tvbep.com", decision.selectedBaseUrl);
    }

    @Test
    public void staysOnPrimaryWhenBothRoutesAreUnavailable() {
        BackendFailoverManager.Decision decision = BackendFailoverManager.evaluate(
                "https://fire.tvbep.com",
                "https://direct.tvbep.com",
                baseUrl -> false,
                delayMs -> { }
        );

        assertFalse(decision.useEmergency);
        assertFalse(decision.emergencyHealthy);
        assertEquals("https://fire.tvbep.com", decision.selectedBaseUrl);
    }

    @Test
    public void switchesAfterFirstPrimaryTransportFailure() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger emergencyCalls = new AtomicInteger();
        BackendFailoverManager.Decision decision = BackendFailoverManager.evaluateDetailed(
                "https://fire.tvbep.com",
                "https://direct.tvbep.com",
                baseUrl -> {
                    if (baseUrl.contains("fire")) {
                        primaryCalls.incrementAndGet();
                        return BackendFailoverManager.ProbeResult.transportFailure();
                    }
                    emergencyCalls.incrementAndGet();
                    return BackendFailoverManager.ProbeResult.healthy();
                },
                delayMs -> { }
        );

        assertTrue(decision.useEmergency);
        assertEquals(1, decision.primaryFailures);
        assertEquals(1, primaryCalls.get());
        assertEquals(1, emergencyCalls.get());
    }

    @Test
    public void directTransportRecoverySkipsAnotherPrimaryProbe() {
        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger emergencyCalls = new AtomicInteger();
        BackendFailoverManager.Decision decision = BackendFailoverManager.evaluateAfterTransportFailure(
                "https://fire.tvbep.com",
                "https://direct.tvbep.com",
                baseUrl -> {
                    if (baseUrl.contains("fire")) {
                        primaryCalls.incrementAndGet();
                        return BackendFailoverManager.ProbeResult.healthy();
                    }
                    emergencyCalls.incrementAndGet();
                    return BackendFailoverManager.ProbeResult.healthy();
                }
        );

        assertTrue(decision.useEmergency);
        assertEquals(0, primaryCalls.get());
        assertEquals(1, emergencyCalls.get());
    }
}
