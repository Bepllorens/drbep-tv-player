package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class QuickChannelListUiFactory {
    interface Host {
        String text(int resId);
        String displayName(ChannelItem item);
        String vodMeta(ChannelItem item);
        String membershipLabel(ChannelItem item, int maxLabels);
        String protectedTitle(ChannelItem item, String title);
        String protectedMeta(ChannelItem item, String meta);
        String protectedTypeBadge(ChannelItem item, String fallback);
        void choose(ChannelItem item);
    }

    private QuickChannelListUiFactory() {
    }

    static QuickChannelListUiModel build(String title, String subtitle, List<ChannelItem> items, Host host) {
        return build(title, subtitle, null, items, null, host);
    }

    static QuickChannelListUiModel build(String title, String subtitle, List<ZapActionItem> actions, List<ChannelItem> items, Host host) {
        return build(title, subtitle, actions, items, null, host);
    }

    static QuickChannelListUiModel build(String title, String subtitle, List<ZapActionItem> actions, List<ChannelItem> items, Runnable onBack, Host host) {
        List<QuickChannelRowUiModel> rows = new ArrayList<>();
        if (host != null && items != null) {
            for (ChannelItem item : items) {
                if (item == null) {
                    continue;
                }
                rows.add(buildRow(host, item));
            }
        }
        return new QuickChannelListUiModel(title, subtitle, actions, rows, onBack);
    }

    private static QuickChannelRowUiModel buildRow(Host host, ChannelItem item) {
        String displayName = host.displayName(item);
        String rowTitle = item.favorite ? "★ " + displayName : displayName;
        String primaryMeta = item.isVod ? host.vodMeta(item) : (item.nowProgram != null && !item.nowProgram.trim().isEmpty() ? item.nowProgram : item.group);
        if (primaryMeta == null || primaryMeta.trim().isEmpty()) {
            primaryMeta = host.text(R.string.search_channel_action_hint);
        }
        String listLabel = host.membershipLabel(item, 2);
        if (!listLabel.isEmpty()) {
            primaryMeta = listLabel + "  ·  " + primaryMeta;
        }
        String typeLabel;
        if (item.isAdultVod) {
            typeLabel = host.text(R.string.channel_badge_vod_adult);
        } else if (item.isVod) {
            typeLabel = host.text(R.string.channel_badge_vod);
        } else {
            typeLabel = host.text(R.string.channel_badge_live);
        }
        return new QuickChannelRowUiModel(
                host.protectedTitle(item, rowTitle),
                host.protectedMeta(item, primaryMeta),
                host.protectedTypeBadge(item, typeLabel),
                item.logoUrl,
                displayName,
                item.isVod,
                () -> host.choose(item)
        );
    }
}
