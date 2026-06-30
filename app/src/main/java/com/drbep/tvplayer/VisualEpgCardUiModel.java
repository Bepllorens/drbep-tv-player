package com.drbep.tvplayer;

public final class VisualEpgCardUiModel {
    public final String title;
    public final String timeLabel;
    public final String badgeLabel;
    public final String posterUrl;
    public final boolean scheduled;
    public final boolean focused;

    public VisualEpgCardUiModel(String title, String timeLabel, String badgeLabel, String posterUrl, boolean scheduled, boolean focused) {
        this.title = title == null ? "" : title;
        this.timeLabel = timeLabel == null ? "" : timeLabel;
        this.badgeLabel = badgeLabel == null ? "" : badgeLabel;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.scheduled = scheduled;
        this.focused = focused;
    }
}
