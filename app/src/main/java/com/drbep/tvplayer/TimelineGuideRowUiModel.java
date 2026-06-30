package com.drbep.tvplayer;

import java.util.List;

public final class TimelineGuideRowUiModel {
    public final String channelName;
    public final String logoUrl;
    public final int labelWidthPx;
    public final List<TimelineGuideBlockUiModel> blocks;

    public TimelineGuideRowUiModel(String channelName, String logoUrl, int labelWidthPx, List<TimelineGuideBlockUiModel> blocks) {
        this.channelName = channelName == null ? "" : channelName;
        this.logoUrl = logoUrl == null ? "" : logoUrl;
        this.labelWidthPx = labelWidthPx;
        this.blocks = blocks;
    }
}
