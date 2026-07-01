package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProgramInfoPanelUiModel {
    public final String title;
    public final TimelineProgramDetailUiModel detail;
    public final List<TvMessageActionUiModel> actions;

    public ProgramInfoPanelUiModel(String title, TimelineProgramDetailUiModel detail, List<TvMessageActionUiModel> actions) {
        this.title = title == null ? "" : title;
        this.detail = detail == null ? new TimelineProgramDetailUiModel("", "", "", "") : detail;
        this.actions = actions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(actions));
    }
}
