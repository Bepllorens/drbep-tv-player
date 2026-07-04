package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class TvOptionsMenuModel {
    final List<String> options;
    final List<Runnable> actions;
    final String message;

    TvOptionsMenuModel(List<String> options, List<Runnable> actions) {
        this(options, actions, null);
    }

    TvOptionsMenuModel(List<String> options, List<Runnable> actions, String message) {
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
        this.message = message;
    }
}
