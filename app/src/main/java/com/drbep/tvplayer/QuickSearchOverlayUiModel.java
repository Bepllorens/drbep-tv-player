package com.drbep.tvplayer;

public final class QuickSearchOverlayUiModel {
    public final String title;
    public final String query;
    public final String result;
    public final String hint;

    public QuickSearchOverlayUiModel(String title, String query, String result, String hint) {
        this.title = title == null ? "" : title;
        this.query = query == null ? "" : query;
        this.result = result == null ? "" : result;
        this.hint = hint == null ? "" : hint;
    }
}
