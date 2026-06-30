package com.drbep.tvplayer;

import java.util.List;

public final class VodVisualPanelUiModel {
    public final String title;
    public final String subtitle;
    public final String help;
    public final String emptyLabel;
    public final List<VodVisualActionUiModel> actions;
    public final List<VodVisualSectionUiModel> sections;

    public VodVisualPanelUiModel(String title, String subtitle, String help, String emptyLabel, List<VodVisualActionUiModel> actions, List<VodVisualSectionUiModel> sections) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.help = help == null ? "" : help;
        this.emptyLabel = emptyLabel == null ? "" : emptyLabel;
        this.actions = actions;
        this.sections = sections;
    }
}
