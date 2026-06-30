package com.drbep.tvplayer;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class ChannelOverlayUi {
    public static final class NowPlayingModel {
        public final String title;
        public final String meta;
        public final String route;
        public final String quality;
        public final boolean qualityVisible;
        public final String recent;
        public final String contextLabel;

        public NowPlayingModel(String title, String meta, String route, String quality, boolean qualityVisible, String recent, String contextLabel) {
            this.title = title;
            this.meta = meta;
            this.route = route;
            this.quality = quality;
            this.qualityVisible = qualityVisible;
            this.recent = recent;
            this.contextLabel = contextLabel == null ? "" : contextLabel.trim();
        }
    }

    private ChannelOverlayUi() {
    }

    static NowPlayingModel buildNowPlayingModel(
            Context context,
            ChannelItem currentChannel,
            String channelTitle,
            String profileTag,
            String contextLabel,
            PlayerController.PlaybackDiagnostics diagnostics,
            String compactQualityLabel,
            List<RecentChannelsStore.RecentChannelItem> recentItems
    ) {
        String title = currentChannel == null
                ? context.getString(R.string.status_ready)
                : safeTrim(channelTitle);
        String meta = buildProgramSummary(context, currentChannel);
        if (!safeTrim(profileTag).isEmpty()) {
            meta = profileTag.trim() + "  ·  " + meta;
        }

        String routeLabel = diagnostics == null || safeTrim(diagnostics.routeLabel).isEmpty()
                ? context.getString(R.string.diagnostics_state_idle)
                : diagnostics.routeLabel.trim();
        String route = context.getString(R.string.overlay_playback_route, routeLabel);

        String quality = "";
        boolean qualityVisible = false;
        if (!safeTrim(compactQualityLabel).isEmpty()) {
            quality = context.getString(R.string.overlay_playback_quality, compactQualityLabel.trim());
            qualityVisible = true;
        } else if (currentChannel != null
                && diagnostics != null
                && diagnostics.playbackState != null
                && !"IDLE".equalsIgnoreCase(diagnostics.playbackState)) {
            quality = context.getString(R.string.overlay_playback_quality_detecting);
            qualityVisible = true;
        }

        String recent = buildRecentSummary(context, recentItems);
        return new NowPlayingModel(title, meta, route, quality, qualityVisible, recent, contextLabel);
    }

    static String buildQuickCountLabel(Context context, int labelRes, int count) {
        return context.getString(R.string.overlay_count_chip, context.getString(labelRes), count);
    }

    static String buildProgramSummary(Context context, ChannelItem channel) {
        String currentLine = channel == null || safeTrim(channel.nowProgram).isEmpty()
                ? context.getString(R.string.overlay_current_program_empty)
                : context.getString(R.string.overlay_current_program, channel.nowProgram.trim());
        String nextLine = channel == null || safeTrim(channel.nextProgram).isEmpty()
                ? context.getString(R.string.overlay_next_program_empty)
                : context.getString(R.string.overlay_next_program, channel.nextProgram.trim());
        return currentLine + "\n" + nextLine;
    }

    private static String buildRecentSummary(Context context, List<RecentChannelsStore.RecentChannelItem> recentItems) {
        if (recentItems == null || recentItems.isEmpty()) {
            return context.getString(R.string.overlay_recent_channels_empty);
        }
        List<String> names = new ArrayList<>();
        int max = Math.min(4, recentItems.size());
        for (int i = 0; i < max; i++) {
            RecentChannelsStore.RecentChannelItem item = recentItems.get(i);
            if (item != null && !safeTrim(item.channelName).isEmpty()) {
                names.add(item.channelName.trim());
            }
        }
        if (names.isEmpty()) {
            return context.getString(R.string.overlay_recent_channels_empty);
        }
        return context.getString(R.string.overlay_recent_channels, joinLabels(names));
    }

    private static String joinLabels(List<String> labels) {
        StringBuilder builder = new StringBuilder();
        for (String label : labels) {
            if (safeTrim(label).isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("  ·  ");
            }
            builder.append(label.trim());
        }
        return builder.toString();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
