package com.drbep.tvplayer;

public final class VodSectionTextUiModel {
    public final String text;
    public final boolean title;

    public VodSectionTextUiModel(String text, boolean title) {
        this.text = text == null ? "" : text;
        this.title = title;
    }
}
