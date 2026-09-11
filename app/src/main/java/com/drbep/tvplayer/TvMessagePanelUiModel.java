package com.drbep.tvplayer;

import java.util.List;

public final class TvMessagePanelUiModel {
    public final String title;
    public final String message;
    public final List<TvMessageActionUiModel> actions;
    public final Runnable onBack;
    public final java.util.function.Consumer<android.widget.ImageView> bindPoster;

    public TvMessagePanelUiModel(String title, String message, List<TvMessageActionUiModel> actions) {
        this(title, message, actions, null);
    }

    public TvMessagePanelUiModel(String title, String message, List<TvMessageActionUiModel> actions, Runnable onBack) {
        this(title, message, actions, onBack, null);
    }

    public TvMessagePanelUiModel(String title, String message, List<TvMessageActionUiModel> actions, Runnable onBack,
            java.util.function.Consumer<android.widget.ImageView> bindPoster) {
        this.title = title == null ? "" : title;
        this.message = message == null ? "" : message;
        this.actions = actions;
        this.onBack = onBack;
        this.bindPoster = bindPoster;
    }
}
