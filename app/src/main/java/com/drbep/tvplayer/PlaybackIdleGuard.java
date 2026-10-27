package com.drbep.tvplayer;

/** Monotonic, input-driven safety policy. Network work and channel changes never count as input. */
final class PlaybackIdleGuard {
    enum Event { NONE, WARN, STOP_IDLE, STOP_OUTPUT }
    private long timeoutMs = 240L * 60_000L, warningMs = 60_000L, outputGraceMs = 20_000L;
    private long lastInput, playbackStarted, outputLostAt = -1L;
    private boolean playing, outputAvailable = true, stopOnOutputLoss = true, stopping;
    PlaybackIdleGuard(long now) { lastInput = now; }
    synchronized void configure(int minutes, int warningSeconds, boolean stopOutput, int graceSeconds) {
        if (minutes != 0 && (minutes < 60 || minutes > 1440)) return;
        timeoutMs = minutes * 60_000L;
        warningMs = Math.max(30, Math.min(120, warningSeconds)) * 1000L;
        stopOnOutputLoss = stopOutput;
        outputGraceMs = Math.max(10, Math.min(120, graceSeconds)) * 1000L;
    }
    synchronized void interaction(long now) { lastInput = now; }
    synchronized void playback(boolean active, long now) {
        if (active && !playing) { playbackStarted = now; stopping = false; }
        playing = active;
    }
    synchronized void output(boolean available, long now) {
        if (available == outputAvailable) return;
        outputAvailable = available;
        outputLostAt = available ? -1L : now;
    }
    synchronized long interactionAge(long now) { return Math.max(0L, now - lastInput); }
    synchronized long warningRemainingSeconds(long now) {
        return Math.max(0L, (Math.max(lastInput, playbackStarted) + timeoutMs + warningMs - now + 999L) / 1000L);
    }
    synchronized Event poll(long now) {
        if (!playing || stopping) return Event.NONE;
        if (stopOnOutputLoss && !outputAvailable && outputLostAt >= 0L && now - outputLostAt >= outputGraceMs) {
            stopping = true; return Event.STOP_OUTPUT;
        }
        if (timeoutMs == 0L) return Event.NONE;
        long age = Math.max(0L, now - Math.max(lastInput, playbackStarted));
        if (age >= timeoutMs + warningMs) { stopping = true; return Event.STOP_IDLE; }
        return age >= timeoutMs ? Event.WARN : Event.NONE;
    }
}
