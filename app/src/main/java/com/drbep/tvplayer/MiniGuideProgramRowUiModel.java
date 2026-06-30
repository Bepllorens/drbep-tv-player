package com.drbep.tvplayer;

public final class MiniGuideProgramRowUiModel {
    public final String time;
    public final String badge;
    public final int badgeColor;
    public final String title;
    public final int progress;
    public final String meta;
    public final Runnable onClick;

    public MiniGuideProgramRowUiModel(
            String time,
            String badge,
            int badgeColor,
            String title,
            int progress,
            String meta,
            Runnable onClick
    ) {
        this.time = time == null ? "" : time;
        this.badge = badge == null ? "" : badge;
        this.badgeColor = badgeColor;
        this.title = title == null ? "" : title;
        this.progress = progress;
        this.meta = meta == null ? "" : meta;
        this.onClick = onClick;
    }
}
