package com.drbep.tvplayer;

/** Local to the TV channel list. Uses key event time, not composition state. */
final class TvNavigationCadence {
    private long previousEventMs = -1;
    private boolean rapid;

    void onNavigation(long eventMs, int repeatCount) {
        long interval = eventMs - previousEventMs;
        rapid = repeatCount > 0 || (previousEventMs >= 0 && interval >= 0 && interval < 160);
        previousEventMs = eventMs;
    }

    int durationMs() {
        // Finish before the next repeat rather than perpetually chasing its focus target.
        return rapid ? 40 : 100;
    }
}
