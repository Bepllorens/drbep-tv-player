package com.drbep.tvplayer;

public class MultiViewTileOverlayUiModel {
    public final String label;
    public final boolean visible;
    public final boolean active;
    public final boolean audioVisible;
    public final String audioLabel;

    public MultiViewTileOverlayUiModel(String label, boolean visible, boolean active, boolean audioVisible, String audioLabel) {
        this.label = label == null ? "" : label;
        this.visible = visible;
        this.active = active;
        this.audioVisible = audioVisible;
        this.audioLabel = audioLabel == null ? "" : audioLabel;
    }
}
