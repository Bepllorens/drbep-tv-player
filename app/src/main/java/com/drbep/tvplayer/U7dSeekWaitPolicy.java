package com.drbep.tvplayer;

/** A short grace period lets the existing playlist grow instead of opening another replay. */
final class U7dSeekWaitPolicy {
    static boolean shouldWait(long target, long[] window, int waits) {
        return window != null && window.length == 2 && waits < 2
                && target > window[1] && target - window[1] <= 3000L;
    }
}
