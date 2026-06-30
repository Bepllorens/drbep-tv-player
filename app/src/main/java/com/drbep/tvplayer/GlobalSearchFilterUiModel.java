package com.drbep.tvplayer;

public final class GlobalSearchFilterUiModel {
    public final String label;
    public final boolean selected;
    public final Runnable onClick;

    public GlobalSearchFilterUiModel(String label, boolean selected, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.selected = selected;
        this.onClick = onClick;
    }
}
