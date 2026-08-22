package com.drbep.tvplayer;

final class StartupChannelPolicy {
    private StartupChannelPolicy() {
    }

    static boolean shouldUseCachedFastChannel(String lastLiveChannelId, String cachedChannelId) {
        String last = clean(lastLiveChannelId);
        String cached = clean(cachedChannelId);
        return !cached.isEmpty() && (last.isEmpty() || last.equals(cached));
    }

    static boolean shouldRememberAsLastLive(boolean vod, boolean replay) {
        return !vod && !replay;
    }

    static boolean shouldApplyRemoteLastChannel(String localChannelId, boolean localChannelAvailable) {
        return clean(localChannelId).isEmpty() || !localChannelAvailable;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
