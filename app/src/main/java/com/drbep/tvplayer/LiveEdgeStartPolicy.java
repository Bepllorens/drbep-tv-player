package com.drbep.tvplayer;

import java.util.Locale;

/** Live-edge positioning must never discard a VOD or replay resume point. */
final class LiveEdgeStartPolicy {
    private LiveEdgeStartPolicy() {}

    static boolean shouldForce(String platform, boolean vod, boolean replay,
                               long resumePositionMs, boolean ismHls) {
        return !vod && !replay && resumePositionMs <= 0L && !ismHls
                && platform != null && platform.toLowerCase(Locale.ROOT).contains("movistar");
    }
}
