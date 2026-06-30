package com.drbep.tvplayer;

public final class ZapActionItem {
    final String label;
    final boolean enabled;
    final boolean highlighted;
    final boolean selected;
    final Runnable onClick;
    final Runnable onLongClick;

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, Runnable onClick) {
        this(label, enabled, highlighted, selected, onClick, null);
    }

    public ZapActionItem(String label, boolean enabled, boolean highlighted, boolean selected, Runnable onClick, Runnable onLongClick) {
        this.label = label == null ? "" : label;
        this.enabled = enabled;
        this.highlighted = highlighted;
        this.selected = selected;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }
}
