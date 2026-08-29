package com.drbep.tvplayer;

public final class VodVisualItemUiModel {
    public final String title;
    public final String meta;
    public final String progressLabel;
    public final String posterUrl;
    public final Runnable onClick;
    public final Runnable onMenu;
    public final boolean compactCard;
    public final String badgeTone;

    public VodVisualItemUiModel(String title, String meta, String progressLabel, String posterUrl, Runnable onClick, Runnable onMenu) {
        this(title, meta, progressLabel, posterUrl, onClick, onMenu, false);
    }

    public VodVisualItemUiModel(String title, String meta, String progressLabel, String posterUrl, Runnable onClick, Runnable onMenu, boolean compactCard) {
        this(title, meta, progressLabel, posterUrl, onClick, onMenu, compactCard, "");
    }

    public VodVisualItemUiModel(String title, String meta, String progressLabel, String posterUrl, Runnable onClick, Runnable onMenu, boolean compactCard, String badgeTone) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.progressLabel = progressLabel == null ? "" : progressLabel;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.onClick = onClick;
        this.onMenu = onMenu;
        this.compactCard = compactCard;
        this.badgeTone = badgeTone == null ? "" : badgeTone;
    }
}
