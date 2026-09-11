package com.drbep.tvplayer;

/** Metadata-only comparison; this is not a measurement of audible A/V sync. */
final class AudioTimelineProbe {
    private long firstPtsUs = Long.MIN_VALUE;
    private long samples;
    private long lastPtsUs;

    void reset() { firstPtsUs = Long.MIN_VALUE; samples = 0; lastPtsUs = 0; }

    long observe(long ptsUs, long count, int sampleRate) {
        if (sampleRate <= 0 || count <= 0) return Long.MIN_VALUE;
        if (firstPtsUs == Long.MIN_VALUE || ptsUs < lastPtsUs) {
            reset();
            firstPtsUs = ptsUs;
        }
        long differenceUs = ptsUs - firstPtsUs - samples * 1_000_000L / sampleRate;
        samples += count;
        lastPtsUs = ptsUs;
        return differenceUs;
    }
}
