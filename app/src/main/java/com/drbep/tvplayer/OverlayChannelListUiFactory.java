package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class OverlayChannelListUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String searchQuery();
        List<ChannelItem> channels();
        boolean touchMode();
        int selectedIndex();
        int currentIndex();
        int scrollToIndex();
        int scrollRequestToken();
        String displayName(ChannelItem item);
        String decorateProtectedTitle(ChannelItem item, String title);
        String decorateProtectedMeta(ChannelItem item, String meta);
        String vodMeta(ChannelItem item);
        String membershipLabel(ChannelItem item, int maxLabels);
        String protectedTypeBadge(ChannelItem item, String fallback);
        boolean isProtected(ChannelItem item);
        String profileTag(ChannelItem item);
        void selectAndTune(int position);
        void selectAndToggleFavorite(int position);
        void moveSelection(int delta);
    }

    private static final int DEFAULT_BADGE_TEXT_COLOR = 0xFFDDE8F6;
    private static final int PROTECTED_BADGE_TEXT_COLOR = 0xFFFFE08A;
    private static final int ADULT_VOD_BADGE_TEXT_COLOR = 0xFFFFD6D6;

    private OverlayChannelListUiFactory() {
    }

    static OverlayChannelListUiModel build(Host host) {
        if (host == null) {
            return new OverlayChannelListUiModel(new ArrayList<>(), -1);
        }
        List<OverlayChannelRowUiModel> items = new ArrayList<>();
        String query = host.searchQuery();
        List<ChannelItem> channels = host.channels();
        if (channels != null) {
            for (int position = 0; position < channels.size(); position++) {
                ChannelItem channel = channels.get(position);
                if (channel == null) {
                    continue;
                }
                items.add(buildRow(host, channel, position, query));
            }
        }
        boolean hasQuery = query != null && !query.trim().isEmpty();
        String emptyMessage = hasQuery
                ? host.text(R.string.overlay_no_results_search, query.trim())
                : host.text(R.string.overlay_no_results);
        return new OverlayChannelListUiModel(
                items,
                host.scrollToIndex(),
                host.scrollRequestToken(),
                host.text(R.string.overlay_section_list),
                "",
                "",
                emptyMessage,
                null,
                null,
                () -> host.moveSelection(-1),
                () -> host.moveSelection(1)
        );
    }

    private static OverlayChannelRowUiModel buildRow(Host host, ChannelItem channel, int position, String query) {
        String name = host.decorateProtectedTitle(channel, host.displayName(channel));
        String metaText;
        String badge = "";
        boolean badgeVisible = false;
        int badgeTextColor = DEFAULT_BADGE_TEXT_COLOR;
        if (channel.isVod) {
            metaText = host.decorateProtectedMeta(channel, host.vodMeta(channel));
            String listLabel = host.membershipLabel(channel, 2);
            if (!listLabel.isEmpty()) {
                metaText = listLabel + "  ·  " + metaText;
            }
            badge = host.protectedTypeBadge(channel, channel.isAdultVod ? host.text(R.string.channel_badge_vod_adult) : host.text(R.string.channel_badge_vod));
            badgeVisible = true;
            badgeTextColor = host.isProtected(channel) ? PROTECTED_BADGE_TEXT_COLOR : (channel.isAdultVod ? ADULT_VOD_BADGE_TEXT_COLOR : DEFAULT_BADGE_TEXT_COLOR);
        } else {
            String tag = host.profileTag(channel);
            String listLabel = host.membershipLabel(channel, 2);
            metaText = "";
            if (channel.nowProgram != null && !channel.nowProgram.trim().isEmpty()) {
                metaText = tag.isEmpty() ? channel.nowProgram : tag + "  ·  " + channel.nowProgram;
            } else if (channel.group != null && !channel.group.trim().isEmpty()) {
                metaText = tag.isEmpty() ? channel.group : tag + "  ·  " + channel.group;
            } else if (!tag.isEmpty()) {
                metaText = tag;
            }
            if (!listLabel.isEmpty()) {
                metaText = metaText.isEmpty() ? listLabel : listLabel + "  ·  " + metaText;
            }
            metaText = host.decorateProtectedMeta(channel, metaText);
            if (host.isProtected(channel)) {
                badge = host.text(R.string.parental_lock_pin_badge);
                badgeVisible = true;
                badgeTextColor = PROTECTED_BADGE_TEXT_COLOR;
            }
        }
        final int rowPosition = position;
        return new OverlayChannelRowUiModel(
                channel.logoUrl,
                name,
                metaText,
                badge,
                badgeVisible,
                badgeTextColor,
                host.touchMode() || channel.favorite,
                channel.favorite,
                host.text(channel.favorite ? R.string.overlay_favorite_toggle_on : R.string.overlay_favorite_toggle_off),
                channel.favorite ? 0xFFFFD54F : 0xFFFFFFFF,
                rowPosition == host.selectedIndex(),
                rowPosition == host.currentIndex(),
                channel.isVod,
                query,
                () -> host.selectAndTune(rowPosition),
                host.touchMode() ? () -> host.selectAndToggleFavorite(rowPosition) : null
        );
    }
}
