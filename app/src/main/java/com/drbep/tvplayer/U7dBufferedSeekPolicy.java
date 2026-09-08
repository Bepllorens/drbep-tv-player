package com.drbep.tvplayer;

final class U7dBufferedSeekPolicy {
    private U7dBufferedSeekPolicy() {}
    static boolean contains(long target, long start, long duration) {
        return start >= 0 && duration > 6000 && target >= start + 1000
                && target - start <= duration - 3000;
    }
}
