package com.drbep.tvplayer;

final class StartupChannelPolicy {
    private StartupChannelPolicy() {
    }

    static boolean shouldUseCachedFastChannel(String lastLiveChannelId, String cachedChannelId) {
        String last = clean(lastLiveChannelId);
        String cached = clean(cachedChannelId);
        return !cached.isEmpty() && (last.isEmpty() || last.equals(cached));
    }

    static boolean shouldSkipCachedFastChannel(String requestedChannelId, String requestedAction) {
        String channelId = clean(requestedChannelId);
        String action = clean(requestedAction);
        return !channelId.isEmpty() && !"record".equals(action);
    }

    static String resolveDeferredTargetChannelId(
            String requestedChannelId,
            String requestedAction,
            String selectedChannelId
    ) {
        String requested = clean(requestedChannelId);
        if (!requested.isEmpty() && !"record".equals(clean(requestedAction))) {
            return requested;
        }
        return clean(selectedChannelId);
    }

    static boolean shouldSkipDeferredPlayback(
            String targetChannelId,
            String currentRequestChannelId,
            boolean fastPlaybackStarted,
            String fastPlaybackChannelId
    ) {
        String target = clean(targetChannelId);
        if (target.isEmpty()) {
            return false;
        }
        return target.equals(clean(currentRequestChannelId))
                || (fastPlaybackStarted && target.equals(clean(fastPlaybackChannelId)));
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
