package com.drbep.tvplayer;

import java.util.List;

final class QuickChannelDialogUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
    }

    private QuickChannelDialogUiFactory() {
    }

    static boolean hasItems(List<ChannelItem> items) {
        return items != null && !items.isEmpty();
    }

    static String emptyMessage(String emptyMessage, Host host) {
        return emptyMessage == null || emptyMessage.trim().isEmpty()
                ? host.text(R.string.overlay_no_results)
                : emptyMessage;
    }

    static String subtitle(List<ChannelItem> items, Host host) {
        int count = items == null ? 0 : items.size();
        return host.text(R.string.quick_channel_count, count) + "  ·  " + host.text(R.string.quick_channel_hint);
    }
}
