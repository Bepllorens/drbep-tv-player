package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VisualEpgHeaderUiModel {
    public final String title;
    public final String subtitle;
    public final List<VisualEpgHeaderActionUiModel> actions;

    public VisualEpgHeaderUiModel(String title, String subtitle, List<VisualEpgHeaderActionUiModel> actions) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.actions = actions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public static final class VisualEpgHeaderActionUiModel {
        public final String label;
        public final Runnable onClick;
        public final Runnable onDown;

        public VisualEpgHeaderActionUiModel(String label, Runnable onClick, Runnable onDown) {
            this.label = label == null ? "" : label;
            this.onClick = onClick;
            this.onDown = onDown;
        }
    }
}
