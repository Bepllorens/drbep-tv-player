package com.drbep.tvplayer;

/** Presentation only: never changes the player's seek range or transport. */
final class PlaybackSeekPresentation {
    private PlaybackSeekPresentation() {}

    static PlayerController.PlaybackSeekState forOnDemand(
            PlayerController.PlaybackSeekState state, boolean onDemand) {
        if (state == null || !onDemand || !state.liveCapable) return state;
        return new PlayerController.PlaybackSeekState(
                state.startMs, state.endMs, state.currentMs, state.label, false);
    }
}
