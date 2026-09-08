package com.drbep.tvplayer;

/** Pixel geometry only; never owns players or changes their playback state. */
public final class MultiViewLayoutPolicy {
    private MultiViewLayoutPolicy() { }
    public static int[] bounds(int width, int height, int count, int slot,
                               boolean pip, int primary, int corner, int margin) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (pip && count == 2) {
            if (slot == primary) return new int[]{0, 0, width, height};
            int w = Math.max(1, width * 30 / 100);
            int h = Math.min(height, Math.max(1, w * 9 / 16));
            int inset = Math.max(0, Math.min(margin, Math.min(width - w, height - h)));
            boolean left = corner == 1 || corner == 2;
            boolean top = corner == 2 || corner == 3;
            return new int[]{left ? inset : width - w - inset,
                    top ? inset : height - h - inset, w, h};
        }
        int columns = 2;
        int rows = count > 2 ? 2 : 1;
        int w = Math.max(1, width / columns);
        int h = Math.max(1, height / rows);
        return new int[]{(slot % columns) * w, (slot / columns) * h, w, h};
    }
}
