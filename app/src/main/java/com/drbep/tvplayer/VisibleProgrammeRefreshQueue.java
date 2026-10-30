package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** UI-thread scheduler for mounted subscribers; owns no catalogue or background I/O. */
final class VisibleProgrammeRefreshQueue {
    static final long INTERVAL_MS = 15_000L;
    static final class Registration {
        final long initialDelayMs;
        final Runnable refresh;
        long dueAt;
        Registration(long delay, Runnable refresh) {
            this.initialDelayMs = Math.max(0L, delay);
            this.refresh = refresh;
        }
    }
    private final Map<Registration, Registration> entries = new LinkedHashMap<>();
    private final Consumer<RuntimeException> onFailure;
    private boolean resumed;
    VisibleProgrammeRefreshQueue(Consumer<RuntimeException> onFailure) { this.onFailure = onFailure; }

    Registration register(long now, long initialDelayMs, Runnable refresh) {
        Registration entry = new Registration(initialDelayMs, refresh);
        entry.dueAt = deadline(now, entry.initialDelayMs);
        entries.put(entry, entry);
        return entry;
    }
    void unregister(Registration entry) { entries.remove(entry); }
    int size() { return entries.size(); }
    void resume(long now) {
        resumed = true;
        for (Registration entry : entries.values()) entry.dueAt = deadline(now, entry.initialDelayMs);
    }
    void pause() { resumed = false; }
    void clear() { pause(); entries.clear(); }
    long delayUntilNext(long now) {
        if (!resumed || entries.isEmpty()) return Long.MAX_VALUE;
        long next = Long.MAX_VALUE;
        for (Registration entry : entries.values()) {
            if (entry.dueAt <= now) return 0L;
            next = Math.min(next, entry.dueAt - now);
        }
        return next;
    }
    void runDue(long now) {
        if (!resumed) return;
        // Callbacks may unregister/re-register rows. Never refresh a disposed registration.
        for (Registration entry : new ArrayList<>(entries.values())) {
            if (!resumed) break;
            if (!entries.containsKey(entry) || entry.dueAt > now) continue;
            entry.dueAt = deadline(now, INTERVAL_MS);
            try { entry.refresh.run(); }
            catch (RuntimeException failure) { if (onFailure != null) onFailure.accept(failure); }
        }
    }
    private static long deadline(long now, long delay) {
        return now > Long.MAX_VALUE - delay ? Long.MAX_VALUE : now + delay;
    }
}
