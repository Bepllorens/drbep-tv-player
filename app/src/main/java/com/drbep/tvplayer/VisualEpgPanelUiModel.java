package com.drbep.tvplayer;

import java.util.Collections;
import java.util.List;

public final class VisualEpgPanelUiModel {
    public final VisualEpgHeaderUiModel header;
    public final List<VisualEpgSectionUiModel> sections;
    public final TimelineProgramDetailUiModel detail;

    public VisualEpgPanelUiModel(
            VisualEpgHeaderUiModel header,
            List<VisualEpgSectionUiModel> sections,
            TimelineProgramDetailUiModel detail
    ) {
        this.header = header;
        this.sections = sections == null ? Collections.emptyList() : sections;
        this.detail = detail == null ? new TimelineProgramDetailUiModel("", "", "", "") : detail;
    }
}
