package com.drbep.tvplayer;

public final class VisualEpgEntryUiModel {
    public final VisualEpgCardUiModel card;
    public final boolean preferred;
    public final Runnable onFocus;
    public final Runnable onClick;

    public VisualEpgEntryUiModel(VisualEpgCardUiModel card, boolean preferred, Runnable onFocus, Runnable onClick) {
        this.card = card;
        this.preferred = preferred;
        this.onFocus = onFocus;
        this.onClick = onClick;
    }
}
