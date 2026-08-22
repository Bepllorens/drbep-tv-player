package com.drbep.tvplayer;

final class PlaybackProgressPolicy {
    static final long CHECK_INTERVAL_MS = 4_000L;
    static final long STALL_THRESHOLD_MS = 12_000L;
    static final long RECOVERY_COOLDOWN_MS = 20_000L;
    static final long STABLE_RESET_MS = 60_000L;
    static final int MAX_RECOVERIES_WITHOUT_STABILITY = 3;
    private static final long MIN_PROGRESS_MS = 250L;

    private PlaybackProgressPolicy() {
    }

    static boolean shouldWatch(boolean touchDevice, boolean vod, boolean firstFrameRendered, boolean playWhenReady, boolean ready) {
        return touchDevice && !vod && firstFrameRendered && playWhenReady && ready;
    }

    static boolean hasAdvanced(long previousPositionMs, long currentPositionMs) {
        if (previousPositionMs < 0L || currentPositionMs < 0L) {
            return true;
        }
        return currentPositionMs < previousPositionMs || currentPositionMs - previousPositionMs >= MIN_PROGRESS_MS;
    }

    static boolean shouldRecover(long stalledForMs, int recoveriesWithoutStability, long sinceLastRecoveryMs) {
        return stalledForMs >= STALL_THRESHOLD_MS
                && recoveriesWithoutStability < MAX_RECOVERIES_WITHOUT_STABILITY
                && sinceLastRecoveryMs >= RECOVERY_COOLDOWN_MS;
    }
}
