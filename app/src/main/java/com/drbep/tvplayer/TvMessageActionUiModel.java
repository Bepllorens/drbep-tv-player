package com.drbep.tvplayer;

public final class TvMessageActionUiModel {
    public final String label;
    public final boolean destructive;
    public final Runnable onClick;

    public TvMessageActionUiModel(String label, boolean destructive, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.destructive = destructive;
        this.onClick = onClick;
    }
}
