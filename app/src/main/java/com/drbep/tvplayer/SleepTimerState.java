package com.drbep.tvplayer;

/** Session-only deadline measured with elapsedRealtime; channel/menu changes do not reset it. */
final class SleepTimerState {
    enum Event { NONE, WARN, EXPIRE }
    private long deadline;
    private boolean warned;
    void start(long now, int minutes) {
        if (minutes <= 0 || minutes > 120) throw new IllegalArgumentException("Duración fuera de rango");
        deadline = now + minutes * 60_000L;
        warned = false;
    }
    void extend(long now, int minutes) {
        if (minutes <= 0 || minutes > 120) throw new IllegalArgumentException("Duración fuera de rango");
        deadline = now + Math.min(120 * 60_000L, remaining(now) + minutes * 60_000L);
        warned = false;
    }
    void cancel() { deadline = 0L; warned = false; }
    boolean active() { return deadline != 0L; }
    long remaining(long now) { return active() ? Math.max(0L, deadline - now) : 0L; }
    Event poll(long now) {
        if (!active()) return Event.NONE;
        long left = remaining(now);
        if (left == 0L) { cancel(); return Event.EXPIRE; }
        if (!warned && left <= 30_000L) { warned = true; return Event.WARN; }
        return Event.NONE;
    }
}
