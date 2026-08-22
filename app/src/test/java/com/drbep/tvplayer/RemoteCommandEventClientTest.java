package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RemoteCommandEventClientTest {
    @Test
    public void endpointUsesOfflineEventContract() {
        assertEquals(
                "https://fire.tvbep.com/api/offline/device/events",
                RemoteCommandEventClient.endpoint("https://fire.tvbep.com/")
        );
    }

    @Test
    public void reconnectBackoffIsBounded() {
        assertEquals(1_000L, RemoteCommandEventClient.retryDelayMs(0));
        assertEquals(4_000L, RemoteCommandEventClient.retryDelayMs(2));
        assertEquals(15_000L, RemoteCommandEventClient.retryDelayMs(20));
    }
}
