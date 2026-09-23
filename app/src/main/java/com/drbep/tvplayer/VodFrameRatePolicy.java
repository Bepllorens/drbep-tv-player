package com.drbep.tvplayer;

final class VodFrameRatePolicy {
    // Tight tolerance distinguishes 23.976 from 24 and 59.94 from 60.
    static boolean matches(float fps, float hz) {
        if (!Float.isFinite(fps) || !Float.isFinite(hz) || fps < 10 || fps > 120) return false;
        int multiple = Math.round(hz / fps);
        return multiple >= 1 && multiple <= 5 && Math.abs(hz / multiple - fps) < 0.012f;
    }
    static int choose(float fps, float currentHz, float[] rates) {
        if (matches(fps, currentHz)) return -1;
        int best = -1;
        for (int i = 0; i < rates.length; i++) {
            if (matches(fps, rates[i]) && (best < 0 || rates[i] < rates[best])) best = i;
        }
        return best;
    }
}
