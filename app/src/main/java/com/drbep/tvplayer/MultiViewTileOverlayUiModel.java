package com.drbep.tvplayer;

public class MultiViewTileOverlayUiModel {
    public final String label;
    public final String slotLabel;
    public final String activeLabel;
    public final String hintLabel;
    public final boolean visible;
    public final boolean active;
    public final boolean audioVisible;
    public final String audioLabel;

    public MultiViewTileOverlayUiModel(String label, boolean visible, boolean active, boolean audioVisible, String audioLabel) {
        this(label, "", "", "", visible, active, audioVisible, audioLabel);
    }

    public MultiViewTileOverlayUiModel(String label, String slotLabel, String activeLabel, String hintLabel, boolean visible, boolean active, boolean audioVisible, String audioLabel) {
        this.label = label == null ? "" : label;
        this.slotLabel = slotLabel == null ? "" : slotLabel;
        this.activeLabel = activeLabel == null ? "" : activeLabel;
        this.hintLabel = hintLabel == null ? "" : hintLabel;
        this.visible = visible;
        this.active = active;
        this.audioVisible = audioVisible;
        this.audioLabel = audioLabel == null ? "" : audioLabel;
    }
}
