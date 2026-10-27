package com.drbep.tvplayer;

/** Constant-space, first-event-only correlation. No URLs or player references. */
final class PlaybackStartupPhaseGate {
    static final int MANIFEST=1, MEDIA=2, VIDEO_DECODER=3;
    private long operation;
    private int generation, recorded;
    synchronized void begin(long operation,int generation) {
        this.operation=operation;this.generation=generation;recorded=0;
    }
    synchronized boolean accept(long operation,int generation,int phase) {
        if(operation<=0 || operation!=this.operation || generation!=this.generation
                || phase<MANIFEST || phase>VIDEO_DECODER)return false;
        int bit=1<<phase;
        if((recorded&bit)!=0)return false;
        recorded|=bit;return true;
    }
}
