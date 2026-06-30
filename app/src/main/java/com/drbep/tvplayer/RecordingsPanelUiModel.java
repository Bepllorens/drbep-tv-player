package com.drbep.tvplayer;

public final class RecordingsPanelUiModel {
    public final String sectionTitle;
    public final String summary;
    public final boolean scheduledMode;
    public final int focusedActionIndex;
    public final Runnable onCompletedClick;
    public final Runnable onScheduledClick;
    public final Runnable onRefreshClick;
    public final String hint;
    public final String posterUrl;
    public final String detailTitle;
    public final String detailMeta;
    public final int detailMetaColor;
    public final String detailPath;
    public final boolean detailPathVisible;
    public final String detailAction;

    public RecordingsPanelUiModel(
            String sectionTitle,
            String summary,
            boolean scheduledMode,
            int focusedActionIndex,
            Runnable onCompletedClick,
            Runnable onScheduledClick,
            Runnable onRefreshClick,
            String hint,
            String posterUrl,
            String detailTitle,
            String detailMeta,
            int detailMetaColor,
            String detailPath,
            boolean detailPathVisible,
            String detailAction
    ) {
        this.sectionTitle = sectionTitle == null ? "" : sectionTitle;
        this.summary = summary == null ? "" : summary;
        this.scheduledMode = scheduledMode;
        this.focusedActionIndex = focusedActionIndex;
        this.onCompletedClick = onCompletedClick;
        this.onScheduledClick = onScheduledClick;
        this.onRefreshClick = onRefreshClick;
        this.hint = hint == null ? "" : hint;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.detailTitle = detailTitle == null ? "" : detailTitle;
        this.detailMeta = detailMeta == null ? "" : detailMeta;
        this.detailMetaColor = detailMetaColor;
        this.detailPath = detailPath == null ? "" : detailPath;
        this.detailPathVisible = detailPathVisible;
        this.detailAction = detailAction == null ? "" : detailAction;
    }
}
