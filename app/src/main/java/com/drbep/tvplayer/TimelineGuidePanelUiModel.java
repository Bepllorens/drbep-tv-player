package com.drbep.tvplayer;

public final class TimelineGuidePanelUiModel {
    public long windowStartMs;
    public long windowEndMs;
    public java.util.List<TimelineHeaderUiModel.TimelineHeaderActionUiModel> days = java.util.Collections.emptyList();
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
