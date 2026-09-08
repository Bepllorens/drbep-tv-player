package com.drbep.tvplayer;

public final class TvOptionsPanelRowUiModel {
    public final String label;
    public final String indexLabel;
    public final Runnable onClick;
    public final java.util.function.Consumer<android.widget.ImageView> bindArtwork;

    public TvOptionsPanelRowUiModel(String label, String indexLabel, Runnable onClick) {
        this(label, indexLabel, onClick, null);
    }

    public TvOptionsPanelRowUiModel(String label, String indexLabel, Runnable onClick,
            java.util.function.Consumer<android.widget.ImageView> bindArtwork) {
        this.label = label == null ? "" : label;
        this.indexLabel = indexLabel == null ? "" : indexLabel;
        this.onClick = onClick;
        this.bindArtwork = bindArtwork;
    }
}
