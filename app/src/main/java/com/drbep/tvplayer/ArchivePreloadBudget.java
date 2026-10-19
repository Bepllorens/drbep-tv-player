package com.drbep.tvplayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** Suppresses speculative repeats only; visible image loads never use this gate. */
final class ArchivePreloadBudget {
    private final int capacity;
    private final long retryMs;
    private final LinkedHashMap<String, Long> attempts = new LinkedHashMap<>();
    ArchivePreloadBudget(int capacity, long retryMs) {
        if (capacity < 1 || retryMs < 1) throw new IllegalArgumentException();
        this.capacity = capacity;
        this.retryMs = retryMs;
    }
    synchronized boolean acquire(String url, long now) {
        if (url == null || url.isEmpty()) return false;
        Long previous = attempts.get(url);
        if (previous != null && now >= previous && now - previous < retryMs) return false;
        attempts.remove(url);
        attempts.put(url, now);
        while (attempts.size() > capacity) {
            String oldest = attempts.keySet().iterator().next();
            attempts.remove(oldest);
        }
        return true;
    }
}
