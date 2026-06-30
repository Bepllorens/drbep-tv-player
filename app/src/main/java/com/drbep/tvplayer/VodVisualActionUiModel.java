package com.drbep.tvplayer;

public final class VodVisualActionUiModel {
    public final String label;
    public final boolean filter;
    public final Runnable onClick;

    public VodVisualActionUiModel(String label, boolean filter, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.filter = filter;
        this.onClick = onClick;
    }
}
