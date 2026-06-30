package com.drbep.tvplayer;

public final class TimelineProgramBlockUiModel {
    public final String title;
    public final String time;
    public final boolean empty;
    public final String statusLabel;

    public TimelineProgramBlockUiModel(String title, String time, boolean empty) {
        this(title, time, empty, "");
    }

    public TimelineProgramBlockUiModel(String title, String time, boolean empty, String statusLabel) {
        this.title = title == null ? "" : title;
        this.time = time == null ? "" : time;
        this.empty = empty;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
    }
}
