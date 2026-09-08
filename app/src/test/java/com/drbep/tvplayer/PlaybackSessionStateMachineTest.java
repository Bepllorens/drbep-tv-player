package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaybackSessionStateMachineTest {
    @Test
    public void mapsPlaybackLifecycleAndCountsOnlyRealTransitions() {
        PlaybackSessionStateMachine machine = new PlaybackSessionStateMachine();

        machine.begin(7, "channel-1", "resolving_stream_info", 100L);
        machine.transition("preparing", 200L);
        machine.transition("buffering", 300L);
        machine.transition("buffering", 400L);
        machine.transition("playing", 500L);

        PlaybackSessionStateMachine.Snapshot snapshot = machine.snapshot();
        assertEquals("7-channel-1", snapshot.sessionId);
        assertEquals(PlaybackSessionStateMachine.State.PLAYING, snapshot.state);
        assertEquals("playing", snapshot.phase);
        assertEquals(4, snapshot.transitionCount);
        assertEquals(500L, snapshot.changedAtElapsedMs);
    }

    @Test
    public void distinguishesNetworkWaitFromRouteRecovery() {
        assertEquals(
                PlaybackSessionStateMachine.State.WAITING_NETWORK,
                PlaybackSessionStateMachine.stateForPhase("waiting_network")
        );
        assertEquals(
                PlaybackSessionStateMachine.State.RECOVERING,
                PlaybackSessionStateMachine.stateForPhase("recovering_stall")
        );
    }
}
