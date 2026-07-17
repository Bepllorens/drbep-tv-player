package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchFilterPickerUiModel {
    public final String title;
    public final String subtitle;
    public final String selectedLabel;
    public final int selectedIndex;
    public final List<TouchFilterPickerRowUiModel> rows;
    public final Runnable onClose;

    public TouchFilterPickerUiModel(String title, String subtitle, String selectedLabel, List<TouchFilterPickerRowUiModel> rows, Runnable onClose) {
        this(title, subtitle, selectedLabel, -1, rows, onClose);
    }

    public TouchFilterPickerUiModel(String title, String subtitle, String selectedLabel, int selectedIndex, List<TouchFilterPickerRowUiModel> rows, Runnable onClose) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.selectedLabel = selectedLabel == null ? "" : selectedLabel;
        this.selectedIndex = selectedIndex;
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        this.onClose = onClose;
    }
}
