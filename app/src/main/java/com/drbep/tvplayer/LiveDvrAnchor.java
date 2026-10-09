package com.drbep.tvplayer;

/** Playback coordinates independent of a rolling manifest's relative origin. */
final class LiveDvrAnchor {
    final String channelId;
    final long programmeTimeMs, behindEdgeMs, capturedElapsedMs;
    final boolean paused;
    LiveDvrAnchor(String channelId,long windowStartMs,long positionMs,long edgeMs,long elapsedMs,boolean paused) {
        this.channelId=channelId;this.programmeTimeMs=windowStartMs>0?windowStartMs+positionMs:-1;
        this.behindEdgeMs=Math.max(0,edgeMs-positionMs);this.capturedElapsedMs=elapsedMs;this.paused=paused;
    }
    boolean shouldRestore() { return paused || behindEdgeMs>15000; }
    Resolution resolve(String channel,long windowStartMs,long endMs,long elapsedMs) {
        if(!channelId.equals(channel)||endMs<=0)return null;
        long target=programmeTimeMs>0&&windowStartMs>0?programmeTimeMs-windowStartMs
            :endMs-behindEdgeMs-Math.max(0,elapsedMs-capturedElapsedMs);
        return new Resolution(Math.max(0,Math.min(endMs,target)),target<0,paused);
    }
    static final class Resolution {
        final long positionMs;final boolean expired,paused;
        Resolution(long positionMs,boolean expired,boolean paused){this.positionMs=positionMs;this.expired=expired;this.paused=paused;}
    }
}
