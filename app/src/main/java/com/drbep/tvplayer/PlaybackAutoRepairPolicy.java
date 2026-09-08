package com.drbep.tvplayer;

final class PlaybackAutoRepairPolicy {
    enum Action {
        NONE,
        RETRY_AUTO,
        RETRY_MODE
    }

    static final class Decision {
        final Action action;
        final String playbackMode;

        private Decision(Action action, String playbackMode) {
            this.action = action == null ? Action.NONE : action;
            this.playbackMode = PlaybackRecoveryCoordinator.sanitizeMode(playbackMode);
        }

        static Decision none() {
            return new Decision(Action.NONE, PlaybackModeStore.MODE_AUTO);
        }
    }

    private PlaybackAutoRepairPolicy() {
    }

    static Decision decide(
            boolean enabled,
            String channelId,
            boolean directPlayback,
            boolean learnedRouteInvalidated,
            String currentMode
    ) {
        if (!enabled || channelId == null || channelId.trim().isEmpty() || directPlayback) {
            return Decision.none();
        }
        if (learnedRouteInvalidated) {
            return new Decision(Action.RETRY_AUTO, PlaybackModeStore.MODE_AUTO);
        }
        String cleanCurrentMode = PlaybackRecoveryCoordinator.sanitizeMode(currentMode);
        if (PlaybackModeStore.MODE_AUTO.equals(cleanCurrentMode)
                || PlaybackModeStore.MODE_PROXY.equals(cleanCurrentMode)) {
            return Decision.none();
        }
        String nextMode = nextMode(cleanCurrentMode);
        if (PlaybackModeStore.MODE_AUTO.equals(nextMode)) {
            return Decision.none();
        }
        return new Decision(Action.RETRY_MODE, nextMode);
    }

    static String nextMode(String currentMode) {
        String clean = PlaybackRecoveryCoordinator.sanitizeMode(currentMode);
        if (PlaybackModeStore.MODE_AUTO.equals(clean)) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        if (PlaybackModeStore.MODE_DIRECT.equals(clean)) {
            return PlaybackModeStore.MODE_PROXY;
        }
        return PlaybackModeStore.MODE_AUTO;
    }
}
