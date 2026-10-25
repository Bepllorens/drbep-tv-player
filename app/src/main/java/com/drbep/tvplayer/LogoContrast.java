package com.drbep.tvplayer;

/** Same alpha, luminance and coverage thresholds as the original logo sampler. */
final class LogoContrast {
    static boolean needsHalo(int[] pixels) {
        int visible=0, dark=0;
        for (int pixel:pixels) {
            if ((pixel >>> 24) < 96) continue;
            visible++;
            if (((pixel >> 16) & 255) * .2126 + ((pixel >> 8) & 255) * .7152
                    + (pixel & 255) * .0722 < 75) dark++;
        }
        return visible > 0 && dark > visible * .65;
    }
    private LogoContrast() { }
}
