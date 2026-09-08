package com.drbep.tvplayer;

final class PlaybackAudioFocusState {
    private boolean resumePending;

    void onPlayWhenReadyChanged(boolean playWhenReady, boolean causedByAudioFocusLoss) {
        if (playWhenReady) {
            resumePending = false;
            return;
        }
        resumePending = causedByAudioFocusLoss;
    }

    boolean consumeResumeRequest(boolean hasPlayableItem) {
        if (!resumePending || !hasPlayableItem) {
            return false;
        }
        resumePending = false;
        return true;
    }

    boolean isResumePending() {
        return resumePending;
    }
}
