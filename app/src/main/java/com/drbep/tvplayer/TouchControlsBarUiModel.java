package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchControlsBarUiModel {
    public final String contextTitle;
    public final String contextSubtitle;
    public final Runnable onContextClick;
    public final List<ZapActionItem> actions;
    public final int focusedActionIndex;
    public final TouchControlsNowPlayingUiModel nowPlaying;

    public TouchControlsBarUiModel(List<ZapActionItem> actions) {
        this("", "", null, actions, 0);
    }

    public TouchControlsBarUiModel(String contextTitle, String contextSubtitle, Runnable onContextClick, List<ZapActionItem> actions) {
        this(contextTitle, contextSubtitle, onContextClick, actions, 0);
    }

    public TouchControlsBarUiModel(String contextTitle, String contextSubtitle, Runnable onContextClick, List<ZapActionItem> actions, int focusedActionIndex) {
        this(contextTitle, contextSubtitle, onContextClick, actions, focusedActionIndex, TouchControlsNowPlayingUiModel.EMPTY);
    }

    public TouchControlsBarUiModel(
            String contextTitle,
            String contextSubtitle,
            Runnable onContextClick,
            List<ZapActionItem> actions,
            int focusedActionIndex,
            TouchControlsNowPlayingUiModel nowPlaying
    ) {
        this.contextTitle = contextTitle == null ? "" : contextTitle;
        this.contextSubtitle = contextSubtitle == null ? "" : contextSubtitle;
        this.onContextClick = onContextClick;
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
        this.focusedActionIndex = Math.max(0, focusedActionIndex);
        this.nowPlaying = nowPlaying == null ? TouchControlsNowPlayingUiModel.EMPTY : nowPlaying;
    }
}
