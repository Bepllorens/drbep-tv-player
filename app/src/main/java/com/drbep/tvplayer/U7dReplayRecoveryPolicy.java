package com.drbep.tvplayer;

/** Reconstruct programme coordinates when a bounded replay window expires. */
final class U7dReplayRecoveryPolicy {
    private U7dReplayRecoveryPolicy() {}

    static long windowPosition(long windowStart, long relativePosition) {
        // A paused position can be negative relative to an advancing live window.
        // Clamp the reconstructed position, not its individual coordinates.
        long start = Math.max(0L, windowStart);
        if (relativePosition > Long.MAX_VALUE - start) return Long.MAX_VALUE;
        return Math.max(0L, start + relativePosition);
    }

    static long absolutePosition(long replayOffset, long localPosition, long duration) {
        if (duration <= 1000L) return 0L;
        long end = duration - 1000L;
        long offset = Math.min(end, Math.max(0L, replayOffset));
        long remaining = end - offset;
        return offset + Math.min(remaining, Math.max(0L, localPosition));
    }
}
