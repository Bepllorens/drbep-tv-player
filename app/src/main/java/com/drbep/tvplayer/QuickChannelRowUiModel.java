package com.drbep.tvplayer;

public final class QuickChannelRowUiModel {
    public final String title;
    public final String meta;
    public final String typeLabel;
    public final String logoUrl;
    public final String channelName;
    public final String channelId;
    public final boolean vod;
    public final boolean playing;
    public final Runnable onClick;

    public QuickChannelRowUiModel(
            String title,
            String meta,
            String typeLabel,
            String logoUrl,
            String channelName,
            String channelId,
            boolean vod,
            boolean playing,
            Runnable onClick
    ) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.typeLabel = typeLabel == null ? "" : typeLabel;
        this.logoUrl = logoUrl == null ? "" : logoUrl;
        this.channelName = channelName == null ? "" : channelName;
        this.channelId = channelId == null ? "" : channelId;
        this.vod = vod;
        this.playing = playing;
        this.onClick = onClick;
    }
}
