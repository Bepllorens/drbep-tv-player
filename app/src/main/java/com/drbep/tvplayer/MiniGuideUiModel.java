package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class MiniGuideUiModel {
    public final String title;
    public final String subtitle;
    public final List<MiniGuideProgramRowUiModel> items;

    public MiniGuideUiModel(List<MiniGuideProgramRowUiModel> items) {
        this(null, null, items);
    }

    public MiniGuideUiModel(String title, String subtitle, List<MiniGuideProgramRowUiModel> items) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
