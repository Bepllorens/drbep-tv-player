package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchFilterPickerUiModel {
    public final String title;
    public final String subtitle;
    public final String selectedLabel;
    public final List<TouchFilterPickerRowUiModel> rows;
    public final Runnable onClose;

    public TouchFilterPickerUiModel(String title, String subtitle, String selectedLabel, List<TouchFilterPickerRowUiModel> rows, Runnable onClose) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.selectedLabel = selectedLabel == null ? "" : selectedLabel;
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        this.onClose = onClose;
    }
}
