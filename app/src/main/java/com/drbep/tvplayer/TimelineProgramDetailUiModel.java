package com.drbep.tvplayer;

public final class TimelineProgramDetailUiModel {
    public final String title;
    public final String meta;
    public final String description;
    public final String imageUrl;
    public final String statusLabel;
    public final String actionHint;

    public TimelineProgramDetailUiModel(String title, String meta, String description, String imageUrl) {
        this(title, meta, description, imageUrl, "", "");
    }

    public TimelineProgramDetailUiModel(String title, String meta, String description, String imageUrl, String statusLabel, String actionHint) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.description = description == null ? "" : description;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.statusLabel = statusLabel == null ? "" : statusLabel;
        this.actionHint = actionHint == null ? "" : actionHint;
    }
}
