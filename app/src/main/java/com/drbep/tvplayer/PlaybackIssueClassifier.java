package com.drbep.tvplayer;

import java.util.Locale;

final class PlaybackIssueClassifier {
    enum IssueType {
        AUTH,
        LICENSE,
        DECODER,
        MANIFEST,
        NETWORK,
        SERVER,
        UNKNOWN
    }

    enum Recommendation {
        REACTIVATE,
        LICENSE,
        DECODER,
        DIRECT,
        PROXY,
        AUTO
    }

    private PlaybackIssueClassifier() {
    }

    static IssueType classify(String message) {
        String error = normalize(message);
        if (isAuthRelated(error)) {
            return IssueType.AUTH;
        }
        if (isLicenseRelated(error)) {
            return IssueType.LICENSE;
        }
        if (isDecoderRelated(error)) {
            return IssueType.DECODER;
        }
        if (containsAny(error, "manifest", "source", "m3u8", "mpd", "404")) {
            return IssueType.MANIFEST;
        }
        if (containsAny(error, "timeout", "timed out", "network", "connect", "dns", "unreachable")) {
            return IssueType.NETWORK;
        }
        if (containsAny(error, "500", "502", "503", "504", "server")) {
            return IssueType.SERVER;
        }
        return IssueType.UNKNOWN;
    }

    static Recommendation recommend(String routeLabel, String playbackMode, String message) {
        String route = normalize(routeLabel);
        String mode = normalize(playbackMode);
        String error = normalize(message);
        if (isAuthRelated(error)) {
            return Recommendation.REACTIVATE;
        }
        if (isLicenseRelated(error)) {
            return Recommendation.LICENSE;
        }
        if (isDecoderRelated(error)) {
            return Recommendation.DECODER;
        }
        if (route.contains("proxy") || mode.contains(PlaybackModeStore.MODE_PROXY) || error.contains("proxy")) {
            return Recommendation.DIRECT;
        }
        if (containsAny(error, "drm", "403", "401", "mime") || route.contains("direct")) {
            return Recommendation.PROXY;
        }
        return Recommendation.AUTO;
    }

    private static boolean isAuthRelated(String error) {
        return containsAny(
                error,
                "401",
                "403",
                "unauthorized",
                "forbidden",
                "token",
                "session",
                "sesion",
                "expired",
                "caduc"
        );
    }

    private static boolean isLicenseRelated(String error) {
        return containsAny(error, "drm", "widevine", "license", "licence", "licencia", "clearkey");
    }

    private static boolean isDecoderRelated(String error) {
        return containsAny(error, "decoder", "mediacodec", "h265", "hevc", "avc", "codec");
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isEmpty() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isEmpty() && value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
