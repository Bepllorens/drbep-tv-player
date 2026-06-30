package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class TouchControlsBarUiModel {
    public final List<ZapActionItem> actions;

    public TouchControlsBarUiModel(List<ZapActionItem> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }
}
