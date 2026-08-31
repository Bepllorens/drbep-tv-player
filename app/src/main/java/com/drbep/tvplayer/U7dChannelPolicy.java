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
        String platform = clean(channel.platformName);
        String identity = platform
                + " " + clean(channel.group)
                + " " + clean(channel.playUrl)
                + " " + clean(channel.fallbackPlayUrl);
        return channel.platformId == 1
                || channel.platformId == 13
                || identity.contains("movistar")
                || identity.contains("movistarplus");
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
        String identity = clean(channel.platformName)
                + " " + clean(channel.group)
                + " " + clean(channel.playUrl)
                + " " + clean(channel.fallbackPlayUrl);
        return channel.platformId == 2 || identity.contains("tivify");
    }

    static boolean isOrangeChannel(ChannelItem channel) {
        if (channel == null || channel.isVod) {
            return false;
        }
        String identity = clean(channel.platformName)
                + " " + clean(channel.group)
                + " " + clean(channel.playUrl)
                + " " + clean(channel.fallbackPlayUrl);
        return identity.contains("orange") || identity.contains("/api/orange/manifest/");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
