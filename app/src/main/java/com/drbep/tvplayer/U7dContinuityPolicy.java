package com.drbep.tvplayer;

/** Playback decisions use programme coordinates, never the sliding segment window. */
final class U7dContinuityPolicy {
    enum Next { NONE, ARCHIVE, LIVE }
    static Next next(long previousEnd, long start, long end, long now) {
        // Only adjoining listings can be continued automatically. A gap requires a new choice.
        if (previousEnd <= 0 || start < previousEnd || start - previousEnd > 60_000L || end <= start || start > now)
            return Next.NONE;
        return end <= now ? Next.ARCHIVE : Next.LIVE;
    }
    static boolean reachedEnd(long position, long duration) {
        return duration > 0 && position >= Math.max(0, duration - 5_000L);
    }
    static boolean canRetry(int attempts, long position, long duration) {
        return attempts < 2 && !reachedEnd(position, duration);
    }
    static long retryPosition(long offset, long local, long duration) {
        return U7dReplayRecoveryPolicy.absolutePosition(offset, local, duration);
    }
    static long freezeTimeout(boolean firstFrame) { return firstFrame ? 12_000L : 25_000L; }
}
