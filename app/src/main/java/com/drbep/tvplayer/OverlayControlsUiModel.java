package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class OverlayControlsUiModel {
    public final String sectionTitle;
    public final String filterLabel;
    public final String searchHint;
    public final String searchQuery;
    public final int searchFocusRequestToken;
    public final int searchClearFocusRequestToken;
    public final SearchQueryChangeHandler onSearchQueryChange;
    public final Runnable onSearchFocused;
    public final List<ZapActionItem> filterActions;
    public final List<ZapActionItem> primaryActions;
    public final List<ZapActionItem> secondaryActions;

    public interface SearchQueryChangeHandler {
        void onSearchQueryChanged(String query);
    }

    public OverlayControlsUiModel(
            String sectionTitle,
            String filterLabel,
            String searchHint,
            String searchQuery,
            int searchFocusRequestToken,
            int searchClearFocusRequestToken,
            SearchQueryChangeHandler onSearchQueryChange,
            Runnable onSearchFocused,
            List<ZapActionItem> filterActions,
            List<ZapActionItem> primaryActions,
            List<ZapActionItem> secondaryActions
    ) {
        this.sectionTitle = sectionTitle == null ? "" : sectionTitle;
        this.filterLabel = filterLabel == null ? "" : filterLabel;
        this.searchHint = searchHint == null ? "" : searchHint;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        this.searchFocusRequestToken = searchFocusRequestToken;
        this.searchClearFocusRequestToken = searchClearFocusRequestToken;
        this.onSearchQueryChange = onSearchQueryChange;
        this.onSearchFocused = onSearchFocused;
        this.filterActions = filterActions == null ? new ArrayList<>() : new ArrayList<>(filterActions);
        this.primaryActions = primaryActions == null ? new ArrayList<>() : new ArrayList<>(primaryActions);
        this.secondaryActions = secondaryActions == null ? new ArrayList<>() : new ArrayList<>(secondaryActions);
    }
}
