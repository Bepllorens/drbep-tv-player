package com.drbep.tvplayer;

import java.util.Locale;

final class U7dChannelPolicy {
    private U7dChannelPolicy() {
    }

    static boolean supports(ChannelItem channel) {
        return isMovistarChannel(channel) || isOrangeChannel(channel);
    }

    static boolean isMovistarChannel(ChannelItem channel) {
        if (channel == null || channel.isVod || clean(channel.name).isEmpty()) {
            return false;
        }
        if (channel.platformId == 1 || channel.platformId == 13) return true;
        return contains(channel.platformName, "movistar") || contains(channel.group, "movistar")
                || contains(channel.playUrl, "movistar") || contains(channel.fallbackPlayUrl, "movistar");
    }

    static boolean isMovistarIsmChannel(ChannelItem channel) {
        if (!isMovistarChannel(channel)) {
            return false;
        }
        String identity = clean(channel.platformName)
                + " " + clean(channel.playUrl)
                + " " + clean(channel.fallbackPlayUrl);
        return identity.contains("ism")
                || identity.contains(".isml/manifest")
                || identity.contains(".ism/manifest")
                || identity.contains("/hls/ism/")
                || identity.contains("/hls/ism-mux/");
    }

    static boolean supportsRecordingStartOver(ChannelItem channel) {
        return isMovistarChannel(channel) || isTivifyChannel(channel);
    }

    static boolean isTivifyChannel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return false;
        }
        return channel.platformId == 2 || contains(channel.platformName, "tivify")
                || contains(channel.group, "tivify") || contains(channel.playUrl, "tivify")
                || contains(channel.fallbackPlayUrl, "tivify");
    }

    static boolean isOrangeChannel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return false;
        }
        return contains(channel.platformName, "orange") || contains(channel.group, "orange")
                || contains(channel.playUrl, "orange") || contains(channel.fallbackPlayUrl, "orange");
    }

    private static boolean contains(String value, String marker) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(marker);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
