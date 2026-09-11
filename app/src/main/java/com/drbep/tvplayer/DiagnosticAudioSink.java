package com.drbep.tvplayer;

import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.ForwardingAudioSink;
import androidx.media3.extractor.Ac3Util;
import java.nio.ByteBuffer;

/** Beta-only, transparent sink. Never records payloads, URLs or credentials. */
@UnstableApi
final class DiagnosticAudioSink extends ForwardingAudioSink {
    private final AudioTimelineProbe probe = new AudioTimelineProbe();
    private int sampleRate;
    private boolean encodedDolby;
    private ByteBuffer pending;
    private long pendingSamples;
    private long lastLogMs;

    DiagnosticAudioSink(AudioSink delegate) { super(delegate); }

    @Override public void configure(Format format, int bufferSize, @Nullable int[] channels)
            throws ConfigurationException {
        super.configure(format, bufferSize, channels);
        clearProbe();
        sampleRate = format.sampleRate;
        encodedDolby = "audio/eac3".equals(format.sampleMimeType)
                || "audio/eac3-joc".equals(format.sampleMimeType)
                || "audio/ac3".equals(format.sampleMimeType);
        Log.w("AudioTimeline", "configured mime=" + format.sampleMimeType
                + " sampleRate=" + sampleRate + " channels=" + format.channelCount);
    }

    @Override public boolean handleBuffer(ByteBuffer buffer, long ptsUs, int unitCount)
            throws InitializationException, WriteException {
        if (encodedDolby && pending != buffer) {
            pending = buffer;
            pendingSamples = 0;
            if (buffer.remaining() >= 6 && unitCount > 0) {
                try {
                    pendingSamples = (long) Ac3Util.parseAc3SyncframeAudioSampleCount(buffer) * unitCount;
                } catch (RuntimeException ignored) { /* Diagnostics must never disrupt playback. */ }
            }
        }
        boolean consumed = super.handleBuffer(buffer, ptsUs, unitCount);
        if (consumed && encodedDolby) {
            long deltaUs = probe.observe(ptsUs, pendingSamples, sampleRate);
            long nowMs = SystemClock.elapsedRealtime();
            if (deltaUs != Long.MIN_VALUE && nowMs - lastLogMs >= 10_000L) {
                Log.w("AudioTimeline", "encodedPtsVsSamplesUs=" + deltaUs
                        + " ptsUs=" + ptsUs + " sampleRate=" + sampleRate);
                lastLogMs = nowMs;
            }
            pending = null;
        }
        return consumed;
    }

    private void clearProbe() { probe.reset(); pending = null; pendingSamples = 0; lastLogMs = 0; }
    @Override public void flush() { super.flush(); clearProbe(); }
    @Override public void reset() { super.reset(); clearProbe(); }
}
