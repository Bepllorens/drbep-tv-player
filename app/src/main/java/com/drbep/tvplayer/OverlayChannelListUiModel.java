package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class OverlayChannelListUiModel {
    public final List<OverlayChannelRowUiModel> items;
    public final int scrollToIndex;
    public final int scrollRequestToken;
    public final String listTitle;
    public final String filterTitle;
    public final String filterLabel;
    public final String emptyMessage;
    public final Runnable onPreviousFilterClick;
    public final Runnable onNextFilterClick;
    public final Runnable onMoveSelectionUp;
    public final Runnable onMoveSelectionDown;

    public OverlayChannelListUiModel(List<OverlayChannelRowUiModel> items, int scrollToIndex) {
        this(items, scrollToIndex, 0, "", "", "", "", null, null, null, null);
    }

    public OverlayChannelListUiModel(
            List<OverlayChannelRowUiModel> items,
            int scrollToIndex,
            int scrollRequestToken,
            String listTitle,
            String filterTitle,
            String filterLabel,
            String emptyMessage,
            Runnable onPreviousFilterClick,
            Runnable onNextFilterClick,
            Runnable onMoveSelectionUp,
            Runnable onMoveSelectionDown
    ) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.scrollToIndex = scrollToIndex;
        this.scrollRequestToken = scrollRequestToken;
        this.listTitle = listTitle == null ? "" : listTitle;
        this.filterTitle = filterTitle == null ? "" : filterTitle;
        this.filterLabel = filterLabel == null ? "" : filterLabel;
        this.emptyMessage = emptyMessage == null ? "" : emptyMessage;
        this.onPreviousFilterClick = onPreviousFilterClick;
        this.onNextFilterClick = onNextFilterClick;
        this.onMoveSelectionUp = onMoveSelectionUp;
        this.onMoveSelectionDown = onMoveSelectionDown;
    }
}
