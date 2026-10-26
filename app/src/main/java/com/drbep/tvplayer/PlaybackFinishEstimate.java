package com.drbep.tvplayer;

/** Wall-clock estimate only for a playing, finite on-demand item. Zero means unavailable. */
final class PlaybackFinishEstimate {
    private static final long MAX_REMAINING_MS=7L*24*60*60*1000;
    private PlaybackFinishEstimate(){}
    static long calculate(long nowMs,long positionMs,long durationMs,double speed,
                          boolean playing,boolean live) {
        if(nowMs<=0 || !playing || live || positionMs<0 || durationMs<=positionMs
                || durationMs>MAX_REMAINING_MS || !Double.isFinite(speed) || speed<=0)return 0;
        double remaining=Math.ceil((durationMs-positionMs)/speed);
        if(!Double.isFinite(remaining) || remaining<=0 || remaining>MAX_REMAINING_MS
                || remaining>Long.MAX_VALUE-nowMs)return 0;
        return nowMs+(long)remaining;
    }
    static long duration(long playerDurationMs,long catalogSeconds) {
        if(playerDurationMs>0)return playerDurationMs;
        return catalogSeconds>0 && catalogSeconds<=Long.MAX_VALUE/1000 ? catalogSeconds*1000L : 0L;
    }
}
