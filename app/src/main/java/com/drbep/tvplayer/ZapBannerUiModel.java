package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class ZapBannerUiModel {
    public final String logoUrl;
    public final String channelBadge;
    public final String channelTitle;
    public final String qualityText;
    public final boolean qualityVisible;
    public final String programTitle;
    public final String programMeta;
    public final String nextProgram;
    public final boolean nextProgramVisible;
    public final String remainingText;
    public final int progress;
    public final boolean progressVisible;
    public final String endTimeText;
    public final List<ZapActionItem> actions;

    public ZapBannerUiModel(
            String logoUrl,
            String channelBadge,
            String channelTitle,
            String qualityText,
            boolean qualityVisible,
            String programTitle,
            String programMeta,
            String nextProgram,
            boolean nextProgramVisible,
            String remainingText,
            int progress,
            boolean progressVisible,
            String endTimeText,
            List<ZapActionItem> actions
    ) {
        this.logoUrl = logoUrl == null ? "" : logoUrl;
        this.channelBadge = channelBadge == null ? "" : channelBadge;
        this.channelTitle = channelTitle == null ? "" : channelTitle;
        this.qualityText = qualityText == null ? "" : qualityText;
        this.qualityVisible = qualityVisible;
        this.programTitle = programTitle == null ? "" : programTitle;
        this.programMeta = programMeta == null ? "" : programMeta;
        this.nextProgram = nextProgram == null ? "" : nextProgram;
        this.nextProgramVisible = nextProgramVisible;
        this.remainingText = remainingText == null ? "" : remainingText;
        this.progress = Math.max(0, Math.min(100, progress));
        this.progressVisible = progressVisible;
        this.endTimeText = endTimeText == null ? "" : endTimeText;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }
}
