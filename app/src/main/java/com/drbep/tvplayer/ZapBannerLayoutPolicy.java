package com.drbep.tvplayer;

final class ZapBannerLayoutPolicy {
    private ZapBannerLayoutPolicy() {
    }

    static boolean useCompactMetrics(int screenWidthDp, int smallestScreenWidthDp) {
        return screenWidthDp < 600 && smallestScreenWidthDp >= 600;
    }

    static boolean stackActions(int screenWidthDp, int smallestScreenWidthDp) {
        return screenWidthDp < 600;
    }

    static boolean showInlineTools(boolean mobileTouchMode) {
        return mobileTouchMode;
    }
}
