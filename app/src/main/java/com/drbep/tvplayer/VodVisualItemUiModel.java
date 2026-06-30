package com.drbep.tvplayer;

public final class VodVisualItemUiModel {
    public final String title;
    public final String meta;
    public final String progressLabel;
    public final String posterUrl;
    public final Runnable onClick;
    public final Runnable onMenu;

    public VodVisualItemUiModel(String title, String meta, String progressLabel, String posterUrl, Runnable onClick, Runnable onMenu) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.progressLabel = progressLabel == null ? "" : progressLabel;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.onClick = onClick;
        this.onMenu = onMenu;
    }
}
