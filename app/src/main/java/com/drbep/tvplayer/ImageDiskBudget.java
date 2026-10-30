package com.drbep.tvplayer;
/** Disk only; RAM limits and decoded images are independent. */
final class ImageDiskBudget {
    static volatile long configuredBytes = -1;
    static final long MIB = 1024L * 1024;
    static long glideBytes(long freeBytes) {
        if (freeBytes <= 0) return 32 * MIB;
        return Math.max(32 * MIB, Math.min(128 * MIB, freeBytes / 50));
    }
}
