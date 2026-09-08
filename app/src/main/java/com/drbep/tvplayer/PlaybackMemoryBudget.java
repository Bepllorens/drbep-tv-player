package com.drbep.tvplayer;

/** Encoded playback buffer target, not a hard limit on total process memory. */
final class PlaybackMemoryBudget {
    private static final long MIB = 1024L * 1024L;
    private static final long BLOCK = 64L * 1024L;

    static int targetBytes(long maxHeapBytes, boolean multiView) {
        if (maxHeapBytes <= 0) throw new IllegalArgumentException("positive heap required");
        // Reserve the majority of the heap for catalog, EPG, UI and temporary work.
        // Four mosaic players share the same fraction as a single full player.
        long singleBudget = Math.min(48L * MIB, maxHeapBytes / 6L);
        long budget = multiView ? singleBudget / 4L : singleBudget;
        return (int) Math.max(BLOCK, budget / BLOCK * BLOCK);
    }

    private PlaybackMemoryBudget() {}
}
