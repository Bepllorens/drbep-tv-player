package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackAudioFocusStateTest {
    @Test
    public void resumesOnlyAfterAudioFocusLoss() {
        PlaybackAudioFocusState state = new PlaybackAudioFocusState();
        state.onPlayWhenReadyChanged(false, true);
        assertTrue(state.isResumePending());
        assertTrue(state.consumeResumeRequest(true));
        assertFalse(state.consumeResumeRequest(true));
    }

    @Test
    public void voluntaryPauseNeverRequestsResume() {
        PlaybackAudioFocusState state = new PlaybackAudioFocusState();
        state.onPlayWhenReadyChanged(false, false);
        assertFalse(state.consumeResumeRequest(true));
    }

    @Test
    public void focusRecoveryOrMissingMediaClearsSafely() {
        PlaybackAudioFocusState state = new PlaybackAudioFocusState();
        state.onPlayWhenReadyChanged(false, true);
        assertFalse(state.consumeResumeRequest(false));
        assertTrue(state.isResumePending());
        state.onPlayWhenReadyChanged(true, false);
        assertFalse(state.isResumePending());
    }
}
