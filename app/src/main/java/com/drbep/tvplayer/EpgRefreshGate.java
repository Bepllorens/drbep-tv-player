package com.drbep.tvplayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** UI-thread gate for automatic HUD hydration. Explicit guide refreshes bypass it. */
final class EpgRefreshGate {
    private static final int MAX_ENTRIES = 256;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    boolean begin(String id, long nowMs) {
        if (id == null || id.trim().isEmpty()) return false;
        Entry entry = entries.get(id);
        if (entry != null && (entry.inFlight || nowMs < entry.retryAtMs)) return false;
        if (entry == null) {
            if (entries.size() >= MAX_ENTRIES) {
                String evict = null;
                for (Map.Entry<String, Entry> candidate : entries.entrySet()) {
                    if (!candidate.getValue().inFlight) { evict = candidate.getKey(); break; }
                }
                if (evict == null) return false;
                entries.remove(evict);
            }
            entry = new Entry();
            entries.put(id, entry);
        }
        entry.inFlight = true;
        return true;
    }

    void complete(String id, long nowMs, boolean hasData, boolean failed) {
        Entry entry = entries.get(id);
        if (entry == null) return;
        entry.inFlight = false;
        entry.misses = hasData ? 0 : Math.min(4, entry.misses + 1);
        long delay = hasData ? 60_000L : failed
                ? Math.min(120_000L, 15_000L << (entry.misses - 1))
                : Math.min(600_000L, 300_000L << (entry.misses - 1));
        entry.retryAtMs = nowMs + delay;
    }

    private static final class Entry {
        boolean inFlight;
        int misses;
        long retryAtMs;
    }
}
