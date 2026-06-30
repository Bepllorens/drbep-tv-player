package com.drbep.tvplayer;

public final class SurfaceBadgeUiModel {
    public final String text;
    public final int backgroundColor;
    public final int textColor;
    public final boolean large;
    public final boolean fullscreen;

    public SurfaceBadgeUiModel(String text, int backgroundColor, int textColor, boolean large, boolean fullscreen) {
        this.text = text == null ? "" : text;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.large = large;
        this.fullscreen = fullscreen;
    }
}
