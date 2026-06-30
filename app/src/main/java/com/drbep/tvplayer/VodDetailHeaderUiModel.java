package com.drbep.tvplayer;

public final class VodDetailHeaderUiModel {
    public final String title;
    public final String meta;
    public final String description;
    public final String progressLabel;
    public final String posterUrl;

    public VodDetailHeaderUiModel(String title, String meta, String description, String progressLabel, String posterUrl) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.description = description == null ? "" : description;
        this.progressLabel = progressLabel == null ? "" : progressLabel;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
    }
}
