package com.drbep.tvplayer;

/** One-shot initial buffering experiment; seeking and recovery keep normal thresholds. */
final class HlsStartupPolicy {
    static final long FAST_BUFFER_US = 500_000L;
    private volatile boolean armed;

    static boolean eligible(boolean experiment, boolean vod, boolean archive, boolean multi,
                            boolean fallback, boolean resumed, String mime) {
        return experiment && !vod && !archive && !multi && !fallback && !resumed
                && ("application/x-mpegURL".equalsIgnoreCase(mime)
                    || "application/vnd.apple.mpegurl".equalsIgnoreCase(mime));
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
