package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GlobalSearchListUiModel {
    public final String title;
    public final String hint;
    public final String query;
    public final List<GlobalSearchFilterUiModel> filters;
    public final Consumer<String> onQueryChanged;
    public final List<GlobalSearchRowUiModel> items;

    public GlobalSearchListUiModel(List<GlobalSearchRowUiModel> items) {
        this("", "", "", new ArrayList<>(), null, items);
    }

    public GlobalSearchListUiModel(
            String title,
            String hint,
            String query,
            List<GlobalSearchFilterUiModel> filters,
            Consumer<String> onQueryChanged,
            List<GlobalSearchRowUiModel> items
    ) {
        this.title = title == null ? "" : title;
        this.hint = hint == null ? "" : hint;
        this.query = query == null ? "" : query;
        this.filters = filters == null ? new ArrayList<>() : new ArrayList<>(filters);
        this.onQueryChanged = onQueryChanged;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
