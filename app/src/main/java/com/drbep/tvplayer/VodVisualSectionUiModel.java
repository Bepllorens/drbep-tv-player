package com.drbep.tvplayer;

import java.util.List;

public final class VodVisualSectionUiModel {
    public final String title;
    public final List<VodVisualItemUiModel> items;

    public VodVisualSectionUiModel(String title, List<VodVisualItemUiModel> items) {
        this.title = title == null ? "" : title;
        this.items = items;
    }
}
