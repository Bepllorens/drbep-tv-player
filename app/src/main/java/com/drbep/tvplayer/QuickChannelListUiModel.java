package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class QuickChannelListUiModel {
    public final String title;
    public final String subtitle;
    public final List<ZapActionItem> actions;
    public final List<QuickChannelRowUiModel> items;
    public final Runnable onBack;

    public QuickChannelListUiModel(List<QuickChannelRowUiModel> items) {
        this(null, null, items);
    }

    public QuickChannelListUiModel(String title, String subtitle, List<QuickChannelRowUiModel> items) {
        this(title, subtitle, null, items);
    }

    public QuickChannelListUiModel(String title, String subtitle, List<ZapActionItem> actions, List<QuickChannelRowUiModel> items) {
        this(title, subtitle, actions, items, null);
    }

    public QuickChannelListUiModel(String title, String subtitle, List<ZapActionItem> actions, List<QuickChannelRowUiModel> items, Runnable onBack) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.onBack = onBack;
    }
}
