package com.drbep.tvplayer;

import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;

/** Delegates all memory/loading decisions; only initial start may be relaxed. */
final class HlsStartupLoadControl implements LoadControl {
    private final LoadControl delegate;
    final HlsStartupPolicy startup = new HlsStartupPolicy();
    private volatile String expectedMediaId = "";
    void arm(boolean eligible, String mediaId) {
        startup.cancel();
        expectedMediaId = mediaId;
        startup.arm(eligible);
    }
    HlsStartupLoadControl(LoadControl delegate) { this.delegate = delegate; }
    @Override public void onPrepared(PlayerId id) { delegate.onPrepared(id); }
    @Override public void onTracksSelected(Parameters p, TrackGroupArray groups, ExoTrackSelection[] tracks) {
        delegate.onTracksSelected(p, groups, tracks);
    }
    @Override public void onStopped(PlayerId id) { startup.cancel(); delegate.onStopped(id); }
    @Override public void onReleased(PlayerId id) { startup.cancel(); delegate.onReleased(id); }
    @Override public Allocator getAllocator(PlayerId id) { return delegate.getAllocator(id); }
    @Override public long getBackBufferDurationUs(PlayerId id) { return delegate.getBackBufferDurationUs(id); }
    @Override public boolean retainBackBufferFromKeyframe(PlayerId id) { return delegate.retainBackBufferFromKeyframe(id); }
    @Override public boolean shouldContinueLoading(Parameters p) { return delegate.shouldContinueLoading(p); }
    @Override public boolean shouldContinuePreloading(PlayerId id, Timeline t, MediaPeriodId period, long buffered) {
        return delegate.shouldContinuePreloading(id, t, period, buffered);
    }
    @Override public boolean shouldStartPlayback(Parameters p) {
        boolean normal = delegate.shouldStartPlayback(p);
        int index = p.timeline.getIndexOfPeriod(p.mediaPeriodId.periodUid);
        if (index < 0) return normal;
        Timeline.Period period = p.timeline.getPeriod(index, new Timeline.Period());
        String id = p.timeline.getWindow(period.windowIndex, new Timeline.Window()).mediaItem.mediaId;
        if (!expectedMediaId.equals(id)) return normal;
        return startup.shouldStart(normal, p.rebuffering,
                p.playWhenReady, p.bufferedDurationUs, p.playbackSpeed);
    }
}
