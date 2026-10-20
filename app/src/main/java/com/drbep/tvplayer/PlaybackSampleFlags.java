package com.drbep.tvplayer;
/** Compact state evidence only; never decides whether recovery is needed. */
final class PlaybackSampleFlags {
    private PlaybackSampleFlags() {}
    static int encode(int state, boolean requested, boolean playing, boolean loading, boolean suppressed) {
        return (state & 7) | (requested?8:0) | (playing?16:0) | (loading?32:0) | (suppressed?64:0);
    }
}
