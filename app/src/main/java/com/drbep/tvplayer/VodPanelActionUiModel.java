package com.drbep.tvplayer;

public final class VodPanelActionUiModel {
    public final String label;
    public final boolean primary;
    public final Runnable onClick;

    public VodPanelActionUiModel(String label, boolean primary, Runnable onClick) {
        this.label = label == null ? "" : label;
        this.primary = primary;
        this.onClick = onClick;
    }
}
