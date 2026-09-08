package com.drbep.tvplayer;

final class PlaybackBackgroundResumeState {
    private boolean resumePending;
    private boolean explicitPauseRequested;
    private boolean systemPauseCandidate;

    void onExplicitPauseRequested() {
        explicitPauseRequested = true;
        systemPauseCandidate = false;
        resumePending = false;
    }

    void onHostPaused(boolean wasPlaying) {
        // Fire OS changes playWhenReady to false immediately before Activity.onPause().
        // Preserve the state that existed before that system transition, while still
        // respecting pauses initiated through the app or MediaSession.
        resumePending = wasPlaying || systemPauseCandidate;
        systemPauseCandidate = false;
    }

    void onPlayWhenReadyChanged(boolean playWhenReady, boolean userReasonPause, boolean remotePause) {
        if (playWhenReady) {
            explicitPauseRequested = false;
            systemPauseCandidate = false;
            resumePending = false;
            return;
        }
        if (remotePause) {
            explicitPauseRequested = false;
            systemPauseCandidate = false;
            resumePending = false;
            return;
        }
        if (userReasonPause) {
            systemPauseCandidate = !explicitPauseRequested;
            explicitPauseRequested = false;
        }
    }

    boolean consumeResumeRequest(boolean hasPlayableItem) {
        if (!resumePending || !hasPlayableItem) {
            return false;
        }
        resumePending = false;
        return true;
    }
}
