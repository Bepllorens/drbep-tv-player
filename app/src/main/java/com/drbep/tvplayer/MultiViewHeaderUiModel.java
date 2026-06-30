package com.drbep.tvplayer;

public class MultiViewHeaderUiModel {
    public final String title;
    public final String hint;
    public final String closeLabel;
    public final Runnable onCloseClick;

    public MultiViewHeaderUiModel(String title, String hint, String closeLabel, Runnable onCloseClick) {
        this.title = title == null ? "" : title;
        this.hint = hint == null ? "" : hint;
        this.closeLabel = closeLabel == null ? "" : closeLabel;
        this.onCloseClick = onCloseClick;
    }
}
