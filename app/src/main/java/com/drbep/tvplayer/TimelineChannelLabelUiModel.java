package com.drbep.tvplayer;

public final class TimelineChannelLabelUiModel {
    public final String name;
    public final String logoUrl;

    public TimelineChannelLabelUiModel(String name, String logoUrl) {
        this.name = name == null ? "" : name;
        this.logoUrl = logoUrl == null ? "" : logoUrl;
    }
}
