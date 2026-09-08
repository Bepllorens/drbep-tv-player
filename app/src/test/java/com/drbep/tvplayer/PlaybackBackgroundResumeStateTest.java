package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackBackgroundResumeStateTest {
    @Test
    public void resumesOnlyWhenPlaybackWasActiveOnPause() {
        PlaybackBackgroundResumeState state = new PlaybackBackgroundResumeState();
        state.onHostPaused(true);
        assertTrue(state.consumeResumeRequest(true));
        assertFalse(state.consumeResumeRequest(true));

        state.onHostPaused(false);
        assertFalse(state.consumeResumeRequest(true));
    }

    @Test
    public void remotePauseCancelsAutomaticResume() {
        PlaybackBackgroundResumeState state = new PlaybackBackgroundResumeState();
        state.onHostPaused(true);
        state.onPlayWhenReadyChanged(false, false, true);
        assertFalse(state.consumeResumeRequest(true));
    }

    @Test
    public void fireSystemPauseImmediatelyBeforeHostPauseStillResumes() {
        PlaybackBackgroundResumeState state = new PlaybackBackgroundResumeState();
        state.onPlayWhenReadyChanged(false, true, false);
        state.onHostPaused(false);
        assertTrue(state.consumeResumeRequest(true));
    }

    @Test
    public void explicitAppPauseIsNotMistakenForFireSystemPause() {
        PlaybackBackgroundResumeState state = new PlaybackBackgroundResumeState();
        state.onExplicitPauseRequested();
        state.onPlayWhenReadyChanged(false, true, false);
        state.onHostPaused(false);
        assertFalse(state.consumeResumeRequest(true));
    }

    @Test
    public void missingMediaKeepsRequestUntilPlaybackIsReady() {
        PlaybackBackgroundResumeState state = new PlaybackBackgroundResumeState();
        state.onHostPaused(true);
        assertFalse(state.consumeResumeRequest(false));
        assertTrue(state.consumeResumeRequest(true));
    }
}
