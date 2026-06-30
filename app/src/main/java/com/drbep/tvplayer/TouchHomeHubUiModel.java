package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchHomeHubUiModel {
    public final String title;
    public final String subtitle;
    public final String libraryTitle;
    public final String accessTitle;
    public final List<ZapActionItem> libraryActions;
    public final List<ZapActionItem> accessActions;

    public TouchHomeHubUiModel(
            String title,
            String subtitle,
            String libraryTitle,
            String accessTitle,
            List<ZapActionItem> libraryActions,
            List<ZapActionItem> accessActions
    ) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.libraryTitle = libraryTitle == null ? "" : libraryTitle;
        this.accessTitle = accessTitle == null ? "" : accessTitle;
        this.libraryActions = libraryActions == null ? new ArrayList<>() : new ArrayList<>(libraryActions);
        this.accessActions = accessActions == null ? new ArrayList<>() : new ArrayList<>(accessActions);
    }
}
