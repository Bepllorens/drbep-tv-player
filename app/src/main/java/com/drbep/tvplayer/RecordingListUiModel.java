package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class RecordingListUiModel {
    public final List<RecordingListRowUiModel> items;
    public final int scrollToIndex;

    public RecordingListUiModel(List<RecordingListRowUiModel> items, int scrollToIndex) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
        this.scrollToIndex = scrollToIndex;
    }
}
