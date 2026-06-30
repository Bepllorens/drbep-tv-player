package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TimelineHeaderUiModel {
    public final String title;
    public final String windowLabel;
    public final List<TimelineHeaderActionUiModel> actions;

    public TimelineHeaderUiModel(String title, String windowLabel) {
        this(title, windowLabel, Collections.emptyList());
    }

    public TimelineHeaderUiModel(String title, String windowLabel, List<TimelineHeaderActionUiModel> actions) {
        this.title = title == null ? "" : title;
        this.windowLabel = windowLabel == null ? "" : windowLabel;
        this.actions = actions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public static final class TimelineHeaderActionUiModel {
        public final String label;
        public final Runnable onClick;
        public final Runnable onDown;

        public TimelineHeaderActionUiModel(String label, Runnable onClick, Runnable onDown) {
            this.label = label == null ? "" : label;
            this.onClick = onClick;
            this.onDown = onDown;
        }
    }
}
