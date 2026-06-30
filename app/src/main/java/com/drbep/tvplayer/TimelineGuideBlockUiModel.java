package com.drbep.tvplayer;

public final class TimelineGuideBlockUiModel {
    public final String title;
    public final String time;
    public final String status;
    public final int spacerWidthPx;
    public final int blockWidthPx;
    public final boolean empty;
    public final boolean live;
    public final boolean scheduled;
    public final boolean preferred;
    public final Runnable onFocus;
    public final Runnable onClick;
    public final Runnable onMenu;

    public TimelineGuideBlockUiModel(String title, String time, String status, int spacerWidthPx, int blockWidthPx, boolean empty, boolean live, boolean scheduled, boolean preferred, Runnable onFocus, Runnable onClick, Runnable onMenu) {
        this.title = title == null ? "" : title;
        this.time = time == null ? "" : time;
        this.status = status == null ? "" : status;
        this.spacerWidthPx = spacerWidthPx;
        this.blockWidthPx = blockWidthPx;
        this.empty = empty;
        this.live = live;
        this.scheduled = scheduled;
        this.preferred = preferred;
        this.onFocus = onFocus;
        this.onClick = onClick;
        this.onMenu = onMenu;
    }
}
