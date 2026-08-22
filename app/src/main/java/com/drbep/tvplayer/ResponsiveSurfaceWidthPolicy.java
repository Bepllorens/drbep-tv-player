package com.drbep.tvplayer;

final class ResponsiveSurfaceWidthPolicy {
    private ResponsiveSurfaceWidthPolicy() {
    }

    static int resolvePanelWidth(
            int screenWidthPx,
            int reservedHorizontalPx,
            int maximumWidthPx,
            int minimumWidthPx,
            boolean expandForPhoneTouch
    ) {
        int availableWidth = Math.max(minimumWidthPx, screenWidthPx - Math.max(0, reservedHorizontalPx));
        if (expandForPhoneTouch) {
            return availableWidth;
        }
        return Math.min(Math.max(minimumWidthPx, maximumWidthPx), availableWidth);
    }
}
