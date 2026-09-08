package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StartupChannelPolicyTest {
    @Test
    public void staleFastCacheCannotReplaceTheLastLiveChannel() {
        assertFalse(StartupChannelPolicy.shouldUseCachedFastChannel("la-2", "la-1"));
        assertTrue(StartupChannelPolicy.shouldUseCachedFastChannel("la-2", "la-2"));
    }

    @Test
    public void explicitlyRequestedPlaybackSkipsTheCachedFastChannel() {
        assertTrue(StartupChannelPolicy.shouldSkipCachedFastChannel("canal-solicitado", "play"));
        assertTrue(StartupChannelPolicy.shouldSkipCachedFastChannel("canal-solicitado", ""));
        assertFalse(StartupChannelPolicy.shouldSkipCachedFastChannel("canal-solicitado", "record"));
        assertFalse(StartupChannelPolicy.shouldSkipCachedFastChannel("", "play"));
    }

    @Test
    public void deferredStartupDoesNotReplayTheSameTarget() {
        assertTrue(StartupChannelPolicy.shouldSkipDeferredPlayback("la-1", "la-1", false, ""));
        assertTrue(StartupChannelPolicy.shouldSkipDeferredPlayback("la-1", "", true, "la-1"));
        assertFalse(StartupChannelPolicy.shouldSkipDeferredPlayback("la-1", "la-2", false, ""));
        assertFalse(StartupChannelPolicy.shouldSkipDeferredPlayback("", "la-1", true, "la-1"));
    }

    @Test
    public void explicitTargetWinsEvenWhenItIsOutsideTheVisibleFilter() {
        assertEquals(
                "tivify-la-sexta",
                StartupChannelPolicy.resolveDeferredTargetChannelId("tivify-la-sexta", "play", "movistar-la-1")
        );
        assertEquals(
                "movistar-la-1",
                StartupChannelPolicy.resolveDeferredTargetChannelId("tivify-la-sexta", "record", "movistar-la-1")
        );
        assertEquals(
                "movistar-la-1",
                StartupChannelPolicy.resolveDeferredTargetChannelId("", "play", "movistar-la-1")
        );
    }

    @Test
    public void vodAndReplayDoNotReplaceTheLastLiveChannel() {
        assertTrue(StartupChannelPolicy.shouldRememberAsLastLive(false, false));
        assertFalse(StartupChannelPolicy.shouldRememberAsLastLive(true, false));
        assertFalse(StartupChannelPolicy.shouldRememberAsLastLive(false, true));
    }

    @Test
    public void remotePreferenceOnlyFillsAMissingOrUnavailableLocalChannel() {
        assertFalse(StartupChannelPolicy.shouldApplyRemoteLastChannel("la-2", true));
        assertTrue(StartupChannelPolicy.shouldApplyRemoteLastChannel("", false));
        assertTrue(StartupChannelPolicy.shouldApplyRemoteLastChannel("retirado", false));
    }
}
