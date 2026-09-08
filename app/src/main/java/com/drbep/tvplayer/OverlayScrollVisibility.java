package com.drbep.tvplayer;

/** Pure viewport policy, independent of Compose and row type. */
final class OverlayScrollVisibility {
    static boolean isFullyVisible(int offset, int size, int start, int end) {
        return size > 0 && end > start && offset >= start
                && (long) offset + size <= end;
    }
}
