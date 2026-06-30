package com.drbep.tvplayer;

public final class TimelineScaleSlotUiModel {
    public final String label;
    public final int textColor;
    public final int widthPx;

    public TimelineScaleSlotUiModel(String label, int textColor, int widthPx) {
        this.label = label == null ? "" : label;
        this.textColor = textColor;
        this.widthPx = widthPx;
    }
}
