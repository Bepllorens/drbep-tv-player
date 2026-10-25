package com.drbep.tvplayer;

/** UI-thread destination while the next lazy row is still being composed. */
final class TvChannelFocusQueue {
    private int pending = -1;

    int move(int focused, int delta, int count) {
        int anchor = pending >= 0 && pending < count ? pending : focused;
        int target = anchor + delta;
        if (target < 0 || target >= count) return -1;
        pending = target;
        return target;
    }

    void focused(int index) { if (pending == index) pending = -1; }
    void cancel() { pending = -1; }
}
