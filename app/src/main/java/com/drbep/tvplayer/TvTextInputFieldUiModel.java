package com.drbep.tvplayer;

public final class TvTextInputFieldUiModel {
    public final String hint;
    public final String initialValue;
    public final boolean password;
    public final boolean numeric;

    public TvTextInputFieldUiModel(String hint, String initialValue, boolean password, boolean numeric) {
        this.hint = hint == null ? "" : hint;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.password = password;
        this.numeric = numeric;
    }
}
