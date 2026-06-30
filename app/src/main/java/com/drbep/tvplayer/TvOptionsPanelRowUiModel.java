package com.drbep.tvplayer;

public final class TvOptionsPanelRowUiModel {
    public final String label;
    public final String indexLabel;
    public final Runnable onClick;

    public TvOptionsPanelRowUiModel(String label, String indexLabel, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.indexLabel = indexLabel == null ? "" : indexLabel;
        this.onClick = onClick;
    }
}
