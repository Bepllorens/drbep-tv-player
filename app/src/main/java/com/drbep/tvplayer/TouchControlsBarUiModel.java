package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchControlsBarUiModel {
    public final String contextTitle;
    public final String contextSubtitle;
    public final Runnable onContextClick;
    public final List<ZapActionItem> actions;

    public TouchControlsBarUiModel(List<ZapActionItem> actions) {
        this("", "", null, actions);
    }

    public TouchControlsBarUiModel(String contextTitle, String contextSubtitle, Runnable onContextClick, List<ZapActionItem> actions) {
        this.contextTitle = contextTitle == null ? "" : contextTitle;
        this.contextSubtitle = contextSubtitle == null ? "" : contextSubtitle;
        this.onContextClick = onContextClick;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }
}
