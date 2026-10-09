package com.drbep.tvplayer;

final class PlaybackRouteLearningPolicy {
    private PlaybackRouteLearningPolicy() {
    }

    // Successful playback refreshes this date; unused workarounds get retried
    // after two weeks so a provider repair is not masked indefinitely.
    static final long LEARNED_ROUTE_TTL_MS = 14L * 24L * 60L * 60L * 1000L;

    static boolean isExpired(long learnedAtMs, long nowMs) {
        return learnedAtMs <= 0L || learnedAtMs > nowMs
                || nowMs - learnedAtMs >= LEARNED_ROUTE_TTL_MS;
    }

    static String effectiveMode(String requestedMode, String routeLabel, boolean usingFallback) {
        if (usingFallback) {
            return PlaybackModeStore.MODE_COMPAT;
        }
        String mode = PlaybackRecoveryCoordinator.sanitizeMode(requestedMode);
        if (!PlaybackModeStore.MODE_AUTO.equals(mode)) {
            return mode;
        }
        String route = routeLabel == null ? "" : routeLabel.trim().toLowerCase(java.util.Locale.ROOT);
        if (route.contains("proxy")) {
            return PlaybackModeStore.MODE_PROXY;
        }
        if (route.contains("direct")) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        return PlaybackModeStore.MODE_AUTO;
    }

    static boolean isStable(String playbackState,
                            boolean playing,
                            boolean firstFrameRendered,
                            String lastError,
                            int bufferingCount,
                            long initialPositionMs,
                            long currentPositionMs,
                            int expectedGeneration,
                            int currentGeneration) {
        boolean playbackAdvanced = Math.abs(currentPositionMs - initialPositionMs) >= 15_000L;
        return "READY".equals(playbackState == null ? "" : playbackState.trim())
                && playing
                && (firstFrameRendered || playbackAdvanced)
                && (lastError == null || lastError.trim().isEmpty())
                && bufferingCount == 0
                && expectedGeneration == currentGeneration;
    }

    static boolean shouldInvalidateAfterFailure(
            String failedMode,
            String configuredMode,
            boolean hasTemporaryOverride,
            String learnedMode
    ) {
        String failed = PlaybackRecoveryCoordinator.sanitizeMode(failedMode);
        return !PlaybackModeStore.MODE_AUTO.equals(failed)
                && PlaybackModeStore.MODE_AUTO.equals(PlaybackRecoveryCoordinator.sanitizeMode(configuredMode))
                && !hasTemporaryOverride
                && failed.equals(PlaybackRecoveryCoordinator.sanitizeMode(learnedMode));
    }

    static boolean shouldInvalidateAfterRecovery(
            String failedMode,
            String recoveredMode,
            String configuredMode,
            boolean hasTemporaryOverride,
            String learnedMode
    ) {
        String failed = PlaybackRecoveryCoordinator.sanitizeMode(failedMode);
        String recovered = PlaybackRecoveryCoordinator.sanitizeMode(recoveredMode);
        return !PlaybackModeStore.MODE_AUTO.equals(recovered)
                && !failed.equals(recovered)
                && shouldInvalidateAfterFailure(failed, configuredMode, hasTemporaryOverride, learnedMode);
    }
}
