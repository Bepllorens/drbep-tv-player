package com.drbep.tvplayer;

public final class ZapActionItem {
    final String label;
    final boolean enabled;
    final boolean highlighted;
    final boolean selected;
    final String iconHint;
    final String iconText;
    final String iconUrl;
    final Runnable onClick;
    final Runnable onLongClick;

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, Runnable onClick) {
        this(label, enabled, highlighted, selected, onClick, null);
    }

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, Runnable onClick, Runnable onLongClick) {
        this(label, enabled, highlighted, selected, "", "", onClick, onLongClick);
    }

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, String iconHint, String iconText, Runnable onClick, Runnable onLongClick) {
        this(label, enabled, highlighted, selected, iconHint, iconText, "", onClick, onLongClick);
    }

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, String iconHint, String iconText, String iconUrl, Runnable onClick, Runnable onLongClick) {
        this.label = label == null ? "" : label;
        this.enabled = enabled;
        this.highlighted = highlighted;
        this.selected = selected;
        this.iconHint = iconHint == null ? "" : iconHint;
        this.iconText = iconText == null ? "" : iconText;
        this.iconUrl = iconUrl == null ? "" : iconUrl;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }
}
