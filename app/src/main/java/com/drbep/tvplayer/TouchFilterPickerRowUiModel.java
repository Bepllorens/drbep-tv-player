package com.drbep.tvplayer;

public final class TouchFilterPickerRowUiModel {
    public final String title;
    public final String subtitle;
    public final String logoUrl;
    public final String logoText;
    public final boolean selected;
    public final boolean locked;
    public final Runnable onClick;

    public TouchFilterPickerRowUiModel(String title, String subtitle, boolean selected, boolean locked, Runnable onClick) {
        this(title, subtitle, "", "", selected, locked, onClick);
    }

    public TouchFilterPickerRowUiModel(String title, String subtitle, String logoUrl, String logoText, boolean selected, boolean locked, Runnable onClick) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.logoUrl = logoUrl == null ? "" : logoUrl.trim();
        this.logoText = logoText == null ? "" : logoText.trim();
        this.selected = selected;
        this.locked = locked;
        this.onClick = onClick;
    }
}
