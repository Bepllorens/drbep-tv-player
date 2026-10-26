package com.drbep.tvplayer;

/** One-shot initial buffering experiment; seeking and recovery keep normal thresholds. */
final class HlsStartupPolicy {
    static final long FAST_BUFFER_US = 500_000L;
    private volatile boolean armed;

    static boolean eligible(boolean experiment, boolean vod, boolean archive, boolean multi,
                            boolean fallback, boolean resumed, String mime) {
        return eligibleForRoute(experiment, vod, archive, multi, fallback, resumed, false, mime);
    }

    static boolean eligibleForRoute(boolean experiment, boolean vod, boolean archive, boolean multi,
                                    boolean fallback, boolean resumed, boolean liveDvr, String mime) {
        // The Movistar HLS backend DVR delivers a continuous TS stream to Media3.
        // This affects only buffering eligibility, never the actual media MIME/source.
        boolean supported = "application/x-mpegURL".equalsIgnoreCase(mime)
                || "application/vnd.apple.mpegurl".equalsIgnoreCase(mime)
                || (liveDvr && "video/mp2t".equalsIgnoreCase(mime));
        return experiment && !vod && !archive && !multi && !fallback && !resumed && supported;
    }

    // DVR is a separate opt-in: the existing plain-HLS experiment never enables it.
    // A restored/seeked archive position keeps the ordinary buffering policy.
    static boolean experimentEnabled(boolean plain, boolean dvr, boolean usesDvr, long targetMs) {
        return usesDvr ? dvr && targetMs <= 0L : plain;
    }

    void arm(boolean eligible) { armed = eligible; }
    void cancel() { armed = false; }

    boolean shouldStart(boolean normal, boolean rebuffering, boolean playWhenReady,
                        long bufferedUs, float speed) {
        if (rebuffering) armed = false;
        boolean fast = armed && !rebuffering && playWhenReady && speed == 1f
                && bufferedUs >= FAST_BUFFER_US;
        if (normal || fast) armed = false;
        return normal || fast;
    }
}
