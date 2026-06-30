package com.drbep.tvplayer;

import java.util.List;

public final class TvOptionsPanelUiModel {
    public final String title;
    public final String message;
    public final String backLabel;
    public final List<TvOptionsPanelRowUiModel> rows;
    public final Runnable onBack;

    public TvOptionsPanelUiModel(String title, String message, String backLabel, List<TvOptionsPanelRowUiModel> rows, Runnable onBack) {
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.backLabel = backLabel == null ? "" : backLabel;
        this.rows = rows;
        this.onBack = onBack;
    }
}
