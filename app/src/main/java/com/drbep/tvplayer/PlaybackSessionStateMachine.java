package com.drbep.tvplayer;

import java.util.Locale;

final class PlaybackSessionStateMachine {
    enum State {
        IDLE,
        RESOLVING,
        PREPARING,
        BUFFERING,
        PLAYING,
        PAUSED,
        RECOVERING,
        WAITING_NETWORK,
        ENDED,
        FAILED
    }

    static final class Snapshot {
        final String sessionId;
        final String channelId;
        final State state;
        final String phase;
        final int transitionCount;
        final long changedAtElapsedMs;

        Snapshot(String sessionId, String channelId, State state, String phase, int transitionCount, long changedAtElapsedMs) {
            this.sessionId = safe(sessionId);
            this.channelId = safe(channelId);
            this.state = state == null ? State.IDLE : state;
            this.phase = safe(phase);
            this.transitionCount = Math.max(0, transitionCount);
            this.changedAtElapsedMs = Math.max(0L, changedAtElapsedMs);
        }
    }

    private String sessionId = "";
    private String channelId = "";
    private State state = State.IDLE;
    private String phase = "idle";
    private int transitionCount;
    private long changedAtElapsedMs;

    void begin(int generation, String nextChannelId, String initialPhase, long nowElapsedMs) {
        sessionId = Math.max(0, generation) + "-" + safe(nextChannelId);
        channelId = safe(nextChannelId);
        transitionCount = 0;
        transition(initialPhase, nowElapsedMs);
    }

    void transition(String nextPhase, long nowElapsedMs) {
        String normalizedPhase = normalizePhase(nextPhase);
        State nextState = stateForPhase(normalizedPhase);
        if (normalizedPhase.equals(phase) && nextState == state) {
            return;
        }
        phase = normalizedPhase;
        state = nextState;
        transitionCount++;
        changedAtElapsedMs = Math.max(0L, nowElapsedMs);
    }

    Snapshot snapshot() {
        return new Snapshot(sessionId, channelId, state, phase, transitionCount, changedAtElapsedMs);
    }

    static State stateForPhase(String value) {
        String phase = normalizePhase(value);
        if (phase.equals("idle")) return State.IDLE;
        if (phase.contains("waiting_network")) return State.WAITING_NETWORK;
        if (phase.startsWith("recovering")) return State.RECOVERING;
        if (phase.contains("resolving")) return State.RESOLVING;
        if (phase.equals("starting") || phase.equals("preparing")) return State.PREPARING;
        if (phase.equals("buffering") || phase.equals("rebuffering") || phase.equals("stalled") || phase.equals("ready_waiting_first_frame")) return State.BUFFERING;
        if (phase.equals("playing")) return State.PLAYING;
        if (phase.equals("paused")) return State.PAUSED;
        if (phase.equals("ended")) return State.ENDED;
        if (phase.equals("error") || phase.equals("failed")) return State.FAILED;
        return State.PREPARING;
    }

    private static String normalizePhase(String value) {
        String clean = safe(value).toLowerCase(Locale.ROOT);
        return clean.isEmpty() ? "idle" : clean;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
