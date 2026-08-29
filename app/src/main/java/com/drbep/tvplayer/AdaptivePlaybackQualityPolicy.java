package com.drbep.tvplayer;

/**
 * Pure state machine for temporary quality caps in automatic live playback.
 *
 * <p>The controller owns persistence and applies the returned level to Media3.
 * Keeping time as an explicit argument makes the behaviour deterministic in
 * unit tests and independent from Android clocks.</p>
 */
final class AdaptivePlaybackQualityPolicy {
    static final int LEVEL_NONE = 0;
    static final int LEVEL_720P = 1;
    static final int LEVEL_540P = 2;
    static final int MAX_LEVEL = LEVEL_540P;

    static final int FAILURES_TO_DOWNGRADE = 2;
    static final long FAILURE_WINDOW_MS = 90_000L;
    static final long STABLE_UPGRADE_MS = 5L * 60L * 1000L;
    static final long RETENTION_MS = 15L * 60L * 1000L;

    private AdaptivePlaybackQualityPolicy() {
    }

    static final class Change {
        final int previousLevel;
        final int level;

        Change(int previousLevel, int level) {
            this.previousLevel = clampLevel(previousLevel);
            this.level = clampLevel(level);
        }

        boolean changed() {
            return previousLevel != level;
        }

        boolean downgraded() {
            return level > previousLevel;
        }

        boolean upgraded() {
            return level < previousLevel;
        }
    }

    static final class State {
        private int level;
        private int failuresInWindow;
        private long failureWindowStartedMs;
        private long stableSinceMs;

        int level() {
            return level;
        }

        int failuresInWindow() {
            return failuresInWindow;
        }

        long stableSinceMs() {
            return stableSinceMs;
        }

        void restoreLevel(int retainedLevel) {
            level = clampLevel(retainedLevel);
            failuresInWindow = 0;
            failureWindowStartedMs = 0L;
            stableSinceMs = 0L;
        }

        void reset() {
            restoreLevel(LEVEL_NONE);
        }

        void resetStabilityWindow() {
            stableSinceMs = 0L;
        }

        Change recordInstability(long nowMs) {
            long safeNowMs = Math.max(1L, nowMs);
            int previousLevel = level;
            stableSinceMs = 0L;
            if (failureWindowStartedMs <= 0L
                    || safeNowMs - failureWindowStartedMs > FAILURE_WINDOW_MS) {
                failureWindowStartedMs = safeNowMs;
                failuresInWindow = 1;
            } else {
                failuresInWindow++;
            }
            if (failuresInWindow >= FAILURES_TO_DOWNGRADE && level < MAX_LEVEL) {
                level++;
                failuresInWindow = 0;
                failureWindowStartedMs = safeNowMs;
            }
            return new Change(previousLevel, level);
        }

        Change recordStable(long nowMs) {
            long safeNowMs = Math.max(1L, nowMs);
            int previousLevel = level;
            if (stableSinceMs <= 0L) {
                stableSinceMs = safeNowMs;
            } else if (level > LEVEL_NONE && safeNowMs - stableSinceMs >= STABLE_UPGRADE_MS) {
                level--;
                stableSinceMs = safeNowMs;
                failuresInWindow = 0;
                failureWindowStartedMs = 0L;
            }
            return new Change(previousLevel, level);
        }
    }

    static boolean isAutomaticMode(String qualityMode) {
        return PlaybackQualityPolicy.AUTO.equals(PlaybackQualityPolicy.normalize(qualityMode));
    }

    static boolean isRetainedLevelValid(int level, long retainedUntilMs, long nowMs) {
        return clampLevel(level) > LEVEL_NONE && retainedUntilMs > Math.max(0L, nowMs);
    }

    static int maxWidth(int level) {
        switch (clampLevel(level)) {
            case LEVEL_720P:
                return 1280;
            case LEVEL_540P:
                return 960;
            default:
                return Integer.MAX_VALUE;
        }
    }

    static int maxHeight(int level) {
        switch (clampLevel(level)) {
            case LEVEL_720P:
                return 720;
            case LEVEL_540P:
                return 540;
            default:
                return Integer.MAX_VALUE;
        }
    }

    static int maxBitrate(int level) {
        switch (clampLevel(level)) {
            case LEVEL_720P:
                return 3_000_000;
            case LEVEL_540P:
                return 1_800_000;
            default:
                return Integer.MAX_VALUE;
        }
    }

    static int clampLevel(int level) {
        return Math.max(LEVEL_NONE, Math.min(MAX_LEVEL, level));
    }
}
