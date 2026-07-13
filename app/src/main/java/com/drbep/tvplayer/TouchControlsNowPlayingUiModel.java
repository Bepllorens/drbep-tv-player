package com.drbep.tvplayer;

public final class TouchControlsNowPlayingUiModel {
    public static final TouchControlsNowPlayingUiModel EMPTY = new TouchControlsNowPlayingUiModel(
            false,
            "",
            "",
            "",
            "",
            "",
            "",
            false,
            "",
            "",
            0,
            false,
            ""
    );

    public final boolean visible;
    public final String logoUrl;
    public final String channelBadge;
    public final String channelName;
    public final String programTitle;
    public final String programMeta;
    public final String nextProgram;
    public final boolean nextProgramVisible;
    public final String posterUrl;
    public final String remainingText;
    public final int progress;
    public final boolean progressVisible;
    public final String endTimeText;

    public TouchControlsNowPlayingUiModel(
            boolean visible,
            String logoUrl,
            String channelBadge,
            String channelName,
            String programTitle,
            String programMeta,
            String nextProgram,
            boolean nextProgramVisible,
            String posterUrl,
            String remainingText,
            int progress,
            boolean progressVisible,
            String endTimeText
    ) {
        this.visible = visible;
        this.logoUrl = logoUrl == null ? "" : logoUrl;
        this.channelBadge = channelBadge == null ? "" : channelBadge;
        this.channelName = channelName == null ? "" : channelName;
        this.programTitle = programTitle == null ? "" : programTitle;
        this.programMeta = programMeta == null ? "" : programMeta;
        this.nextProgram = nextProgram == null ? "" : nextProgram;
        this.nextProgramVisible = nextProgramVisible;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.remainingText = remainingText == null ? "" : remainingText;
        this.progress = Math.max(0, Math.min(100, progress));
        this.progressVisible = progressVisible;
        this.endTimeText = endTimeText == null ? "" : endTimeText;
    }
}
