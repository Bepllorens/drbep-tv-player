package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaybackAutoRepairPolicyTest {
    @Test
    public void invalidLearnedRouteRetriesUsingAutomaticSelection() {
        PlaybackAutoRepairPolicy.Decision decision = PlaybackAutoRepairPolicy.decide(
                true,
                "1103938",
                false,
                true,
                PlaybackModeStore.MODE_DIRECT
        );

        assertEquals(PlaybackAutoRepairPolicy.Action.RETRY_AUTO, decision.action);
        assertEquals(PlaybackModeStore.MODE_AUTO, decision.playbackMode);
    }

    @Test
    public void explicitDirectFailureTriesProxyOnlyOnceAtPolicyLevel() {
        PlaybackAutoRepairPolicy.Decision decision = PlaybackAutoRepairPolicy.decide(
                true,
                "1103938",
                false,
                false,
                PlaybackModeStore.MODE_DIRECT
        );

        assertEquals(PlaybackAutoRepairPolicy.Action.RETRY_MODE, decision.action);
        assertEquals(PlaybackModeStore.MODE_PROXY, decision.playbackMode);
    }

    @Test
    public void automaticProxyAndCompatibilityFailuresDoNotLoop() {
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(true, "1103938", false, false, PlaybackModeStore.MODE_AUTO).action
        );
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(true, "1103938", false, false, PlaybackModeStore.MODE_PROXY).action
        );
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(true, "1103938", false, false, PlaybackModeStore.MODE_COMPAT).action
        );
    }

    @Test
    public void disabledInvalidAndDirectPlaybackRequestsNeverRepair() {
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(false, "1103938", false, true, PlaybackModeStore.MODE_DIRECT).action
        );
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(true, " ", false, true, PlaybackModeStore.MODE_DIRECT).action
        );
        assertEquals(
                PlaybackAutoRepairPolicy.Action.NONE,
                PlaybackAutoRepairPolicy.decide(true, "1103938", true, true, PlaybackModeStore.MODE_DIRECT).action
        );
    }

    @Test
    public void manualRouteCycleRemainsDeterministic() {
        assertEquals(PlaybackModeStore.MODE_DIRECT, PlaybackAutoRepairPolicy.nextMode(PlaybackModeStore.MODE_AUTO));
        assertEquals(PlaybackModeStore.MODE_PROXY, PlaybackAutoRepairPolicy.nextMode(PlaybackModeStore.MODE_DIRECT));
        assertEquals(PlaybackModeStore.MODE_AUTO, PlaybackAutoRepairPolicy.nextMode(PlaybackModeStore.MODE_PROXY));
        assertEquals(PlaybackModeStore.MODE_AUTO, PlaybackAutoRepairPolicy.nextMode(PlaybackModeStore.MODE_COMPAT));
    }
}
