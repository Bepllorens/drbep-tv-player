package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class VodDetailPanelUiModel {
    public final String title;
    public final String meta;
    public final String description;
    public final String progressLabel;
    public final String posterUrl;
    public final String primaryTitle;
    public final String secondaryTitle;
    public final String hint;
    public final List<VodPanelActionUiModel> primaryActions;
    public final List<VodPanelActionUiModel> secondaryActions;

    public VodDetailPanelUiModel(
            String title,
            String meta,
            String description,
            String progressLabel,
            String posterUrl,
            String primaryTitle,
            String secondaryTitle,
            String hint,
            List<VodPanelActionUiModel> primaryActions,
            List<VodPanelActionUiModel> secondaryActions
    ) {
        this.title = title == null ? "" : title;
        this.meta = meta == null ? "" : meta;
        this.description = description == null ? "" : description;
        this.progressLabel = progressLabel == null ? "" : progressLabel;
        this.posterUrl = posterUrl == null ? "" : posterUrl;
        this.primaryTitle = primaryTitle == null ? "" : primaryTitle;
        this.secondaryTitle = secondaryTitle == null ? "" : secondaryTitle;
        this.hint = hint == null ? "" : hint;
        this.primaryActions = primaryActions == null ? new ArrayList<>() : new ArrayList<>(primaryActions);
        this.secondaryActions = secondaryActions == null ? new ArrayList<>() : new ArrayList<>(secondaryActions);
    }
}
