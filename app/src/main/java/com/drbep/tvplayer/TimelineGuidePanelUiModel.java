package com.drbep.tvplayer;

public final class TimelineGuidePanelUiModel {
    public final TimelineHeaderUiModel header;
    public final TimelineScaleUiModel scale;
    public final TimelineGuideRowsUiModel rows;
    public final TimelineProgramDetailUiModel detail;

    public TimelineGuidePanelUiModel(
            TimelineHeaderUiModel header,
            TimelineScaleUiModel scale,
            TimelineGuideRowsUiModel rows,
            TimelineProgramDetailUiModel detail
    ) {
        this.header = header;
        this.scale = scale;
        this.rows = rows;
        this.detail = detail == null ? new TimelineProgramDetailUiModel("", "", "", "") : detail;
    }
}
