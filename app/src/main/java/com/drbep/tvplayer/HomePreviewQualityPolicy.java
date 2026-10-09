package com.drbep.tvplayer;

/** Bound adaptive home previews without changing the full-screen quality preference. */
final class HomePreviewQualityPolicy {
    static int[] limits(boolean preview, int width, int height, int bitrate) {
        return preview ? new int[]{Math.min(width, 1280), Math.min(height, 720), Math.min(bitrate, 2500000)}
                : new int[]{width, height, bitrate};
    }
    private HomePreviewQualityPolicy() { }
}
