package com.drbep.tvplayer;

public final class RecordingListRowUiModel {
    public final String title;
    public final String meta;
    public final int metaColor;
    public final String statusLabel;
    public final int statusBadgeColor;
    public final String posterUrl;
    public final boolean selected;
    public final Runnable onClick;

    public RecordingListRowUiModel(
            String title,
            String meta,
            int metaColor,
            String statusLabel,
            int statusBadgeColor,
            String posterUrl,
            boolean selected,
            Runnable onClick
    ) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.metaColor = metaColor;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
        this.statusBadgeColor = statusBadgeColor;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.selected = selected;
        this.onClick = onClick;
    }
}
