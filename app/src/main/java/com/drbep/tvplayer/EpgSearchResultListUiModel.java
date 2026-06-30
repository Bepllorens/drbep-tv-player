package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class EpgSearchResultListUiModel {
    public final String title;
    public final String subtitle;
    public final List<EpgSearchResultRowUiModel> items;

    public EpgSearchResultListUiModel(List<EpgSearchResultRowUiModel> items) {
        this(null, null, items);
    }

    public EpgSearchResultListUiModel(String title, String subtitle, List<EpgSearchResultRowUiModel> items) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
