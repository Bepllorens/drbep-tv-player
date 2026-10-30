package com.drbep.tvplayer;

/** One current resolution per controller; contains no playback identifiers or credentials. */
final class PlaybackResolutionTrace {
    private int generation;
    private long operation, started;
    synchronized void begin(int generation, long operation, long now) {
        this.generation=generation;this.operation=operation;this.started=now;
    }
    synchronized long[] link(int generation, long now) {
        if(operation<=0 || this.generation!=generation || now<started)return null;
        return new long[]{operation,now-started,generation};
    }
}
