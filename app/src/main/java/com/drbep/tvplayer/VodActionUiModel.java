package com.drbep.tvplayer;

public final class VodActionUiModel {
    public final String label;
    public final boolean primary;
    public final boolean focused;

    public VodActionUiModel(String label, boolean primary, boolean focused) {
        this.label = label == null ? "" : label;
        this.primary = primary;
        this.focused = focused;
    }
}
