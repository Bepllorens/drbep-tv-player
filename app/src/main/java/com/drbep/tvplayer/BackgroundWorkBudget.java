package com.drbep.tvplayer;

/** Cooperative quiet periods; never blocks the UI or playback and never cancels user work. */
final class BackgroundWorkBudget {
    static final BackgroundWorkBudget INSTANCE = new BackgroundWorkBudget();
    private long quietUntil;
    synchronized void defer(long now, long durationMs) {
        quietUntil = Math.max(quietUntil, now + Math.max(0, durationMs));
    }
    synchronized long remaining(long now) { return Math.max(0, quietUntil - now); }
    static long now() { return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()); }
    static void interaction() { INSTANCE.defer(now(), 750); }
    static void startup() { INSTANCE.defer(now(), 8000); }
    static boolean busy() { return INSTANCE.remaining(now()) > 0; }
}
