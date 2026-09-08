package com.drbep.tvplayer;

/** Budgets for two independent decoders, not a request to upscale the source. */
final class MultiViewQualityPolicy {
    static final int MOSAIC = 0, PRIMARY = 1, THUMBNAIL = 2;
    private MultiViewQualityPolicy() { }
    static int role(boolean pip, int slot, int primary) {
        return pip ? (slot == primary ? PRIMARY : THUMBNAIL) : MOSAIC;
    }
    static int[] limits(int role) {
        if (role == PRIMARY) return new int[]{1280, 720, 3_000_000};
        if (role == THUMBNAIL) return new int[]{640, 360, 800_000};
        return new int[]{960, 540, 1_800_000};
    }
}
