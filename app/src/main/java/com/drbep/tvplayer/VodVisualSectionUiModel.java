package com.drbep.tvplayer;

import java.util.List;

public final class VodVisualSectionUiModel {
    public final String title;
    public final String subtitle;
    public final List<VodVisualItemUiModel> items;

    public VodVisualSectionUiModel(String title, List<VodVisualItemUiModel> items) {
        this(title, "", items);
    }

    public VodVisualSectionUiModel(String title, String subtitle, List<VodVisualItemUiModel> items) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.items = items;
    }
}
