package com.drbep.tvplayer;

public final class TimeshiftBarUiModel {
    public interface PreviewLabelProvider {
        String provide(int progress);
    }

    public interface SeekCommitHandler {
        void seekTo(int progress);
    }

    public final String statusLabel;
    public final int progress;
    public final boolean liveVisible;
    public final Runnable onLiveClick;
    public final Runnable onSeekStart;
    public final PreviewLabelProvider previewLabelProvider;
    public final SeekCommitHandler seekCommitHandler;

    public TimeshiftBarUiModel(
            String statusLabel,
            int progress,
            boolean liveVisible,
            Runnable onLiveClick,
            Runnable onSeekStart,
            PreviewLabelProvider previewLabelProvider,
            SeekCommitHandler seekCommitHandler
    ) {
        this.statusLabel = statusLabel == null ? "" : statusLabel;
        this.progress = Math.max(0, Math.min(1000, progress));
        this.liveVisible = liveVisible;
        this.onLiveClick = onLiveClick;
        this.onSeekStart = onSeekStart;
        this.previewLabelProvider = previewLabelProvider;
        this.seekCommitHandler = seekCommitHandler;
    }
}
