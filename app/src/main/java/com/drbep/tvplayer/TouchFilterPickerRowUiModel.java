package com.drbep.tvplayer;

public final class TouchFilterPickerRowUiModel {
    public final String title;
    public final String subtitle;
    public final boolean selected;
    public final boolean locked;
    public final Runnable onClick;

    public TouchFilterPickerRowUiModel(String title, String subtitle, boolean selected, boolean locked, Runnable onClick) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.selected = selected;
        this.locked = locked;
        this.onClick = onClick;
    }
}
