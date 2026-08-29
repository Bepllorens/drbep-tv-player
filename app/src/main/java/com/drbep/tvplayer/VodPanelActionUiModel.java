package com.drbep.tvplayer;

public final class VodPanelActionUiModel {
    public final String label;
    public final boolean primary;
    public final Runnable onClick;
    public final long availableAtMs;
    public final String waitingLabelPrefix;
    public final String availableLabel;
    public final String tone;

    public VodPanelActionUiModel(String label, boolean primary, Runnable onClick) {
        this(label, primary, onClick, 0L, "", "");
    }

    public VodPanelActionUiModel(String label, boolean primary, Runnable onClick, String tone) {
        this(label, primary, onClick, 0L, "", "", tone);
    }

    public VodPanelActionUiModel(
            String label,
            boolean primary,
            Runnable onClick,
            long availableAtMs,
            String waitingLabelPrefix,
            String availableLabel
    ) {
        this(label, primary, onClick, availableAtMs, waitingLabelPrefix, availableLabel, "");
    }

    public VodPanelActionUiModel(
            String label,
            boolean primary,
            Runnable onClick,
            long availableAtMs,
            String waitingLabelPrefix,
            String availableLabel,
            String tone
    ) {
        this.label = label == null ? "" : label;
        this.primary = primary;
        this.onClick = onClick;
        this.availableAtMs = Math.max(0L, availableAtMs);
        this.waitingLabelPrefix = waitingLabelPrefix == null ? "" : waitingLabelPrefix;
        this.availableLabel = availableLabel == null ? "" : availableLabel;
        this.tone = tone == null ? "" : tone;
    }

    public boolean isEnabledAt(long nowMs) {
        return onClick != null && (availableAtMs <= 0L || nowMs >= availableAtMs);
    }

    public String labelAt(long nowMs) {
        if (availableAtMs <= 0L) {
            return label;
        }
        if (nowMs >= availableAtMs) {
            return availableLabel.isEmpty() ? label : availableLabel;
        }
        long totalSeconds = Math.max(1L, (availableAtMs - nowMs + 999L) / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        String remaining = days > 0L
                ? String.format(java.util.Locale.ROOT, "%d d %02d:%02d:%02d", days, hours, minutes, seconds)
                : String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        String prefix = waitingLabelPrefix.isEmpty() ? label : waitingLabelPrefix;
        return prefix + " " + remaining;
    }
}
