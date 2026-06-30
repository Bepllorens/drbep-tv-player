package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class QuickChannelListUiModel {
    public final String title;
    public final String subtitle;
    public final List<QuickChannelRowUiModel> items;

    public QuickChannelListUiModel(List<QuickChannelRowUiModel> items) {
        this(null, null, items);
    }

    public QuickChannelListUiModel(String title, String subtitle, List<QuickChannelRowUiModel> items) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
