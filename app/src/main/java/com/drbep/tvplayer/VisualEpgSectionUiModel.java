package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VisualEpgSectionUiModel {
    public final String title;
    public final List<VisualEpgEntryUiModel> entries;

    public VisualEpgSectionUiModel(String title, List<VisualEpgEntryUiModel> entries) {
        this.title = title == null ? "" : title;
        this.entries = entries == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(entries));
    }
}
