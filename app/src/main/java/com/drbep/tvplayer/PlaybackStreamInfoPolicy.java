package com.drbep.tvplayer;

import java.util.Locale;

final class PlaybackStreamInfoPolicy {
    private PlaybackStreamInfoPolicy() {
    }

    static boolean shouldResolveBeforePlayback(boolean standaloneMode, ChannelItem channel, PlayerController.PlaybackRequest request, String displayName) {
        if (!standaloneMode || request == null || channel == null || channel.isVod) {
            return false;
        }
        if (request.directPlayback) {
            return shouldResolveDirectDrmBeforePlayback(channel, request);
        }
        if (isOrangePlaybackRequest(channel, request)) {
            return true;
        }
        return isMovistarIsmPlaybackRequest(channel, request, displayName);
    }

    static boolean isMovistarIsmPlaybackRequest(ChannelItem channel, PlayerController.PlaybackRequest request, String displayName) {
        String platform = channel == null ? "" : safeLower(channel.platformName);
        String group = channel == null ? "" : safeLower(channel.group);
        String name = safeLower(displayName);
        String playUrl = request == null ? "" : safeLower(request.playUrl);
        String fallbackUrl = request == null ? "" : safeLower(request.fallbackPlayUrl);
        boolean movistar = platform.contains("movistar ism")
                || (platform.contains("movistar") && (group.contains("movistar") || name.contains("dazn")))
                || playUrl.contains("movistarplus")
                || isMovistarIsmBackendUrl(fallbackUrl);
        boolean smooth = playUrl.contains(".isml/manifest")
                || playUrl.contains(".ism/manifest")
                || isMovistarIsmBackendUrl(fallbackUrl)
                || platform.contains("ism");
        return movistar && smooth;
    }

    static boolean isOrangePlaybackRequest(ChannelItem channel, PlayerController.PlaybackRequest request) {
        String platform = channel == null ? "" : safeLower(channel.platformName);
        String group = channel == null ? "" : safeLower(channel.group);
        String playUrl = request == null ? "" : safeLower(request.playUrl);
        String fallbackUrl = request == null ? "" : safeLower(request.fallbackPlayUrl);
        return platform.contains("orange")
                || group.contains("orange")
                || playUrl.contains("/orange/")
                || fallbackUrl.contains("/orange/");
    }

    static boolean shouldResolveDirectDrmBeforePlayback(ChannelItem channel, PlayerController.PlaybackRequest request) {
        if (channel == null || request == null || !request.directPlayback) {
            return false;
        }
        if (safeLower(request.platformName).contains("movistar hls")) {
            return true;
        }
        String drm = safeLower(channel.drmScheme);
        String playUrl = safeLower(request.playUrl);
        if (!"clearkey".equals(drm) && !"widevine".equals(drm)) {
            return false;
        }
        if (PlayerController.isSecureDrmReference(request.drmLicenseUrl)) {
            return true;
        }
        if ("clearkey".equals(drm) && playUrl.contains(".mpd")) {
            return true;
        }
        return "widevine".equals(drm)
                && (request.drmLicenseUrl == null || request.drmLicenseUrl.trim().isEmpty());
    }

    private static boolean isMovistarIsmBackendUrl(String url) {
        return url != null
                && (url.contains("/hls/ism/")
                || url.contains("/hls/ism-mux/"));
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
