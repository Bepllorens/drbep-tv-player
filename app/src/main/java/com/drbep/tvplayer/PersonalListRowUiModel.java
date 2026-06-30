package com.drbep.tvplayer;

public final class PersonalListRowUiModel {
    public final String badge;
    public final String title;
    public final String preview;
    public final String actionLabel;
    public final Runnable onClick;
    public final Runnable onLongClick;

    public PersonalListRowUiModel(
            String badge,
            String title,
            String preview,
            String actionLabel,
            Runnable onClick,
            Runnable onLongClick
    ) {
        this.badge = badge == null ? "" : badge;
        this.title = title == null ? "" : title;
        this.preview = preview == null ? "" : preview;
        this.actionLabel = actionLabel == null ? "" : actionLabel;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }
}
