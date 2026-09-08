package com.drbep.tvplayer;

final class TimeshiftUiFactory {
    interface Host {
        String statusLabel(PlayerController.PlaybackSeekState state);
        String previewLabel(PlayerController.PlaybackSeekState state, long previewTargetMs);
        void showControls();
        boolean resumeLive();
        void showUnavailable();
        void update();
        void markDragging(boolean dragging);
        void seekTo(long targetMs);
        void scheduleAutoHide();
    }

    private TimeshiftUiFactory() {
    }

    static TimeshiftBarUiModel build(PlayerController.PlaybackSeekState state, Host host) {
        if (state == null || host == null) {
            return new TimeshiftBarUiModel("", 0, false, null, null, null, null);
        }
        long range = Math.max(1L, state.endMs - state.startMs);
        int progress = (int) Math.max(0L, Math.min(1000L, Math.round(((state.currentMs - state.startMs) * 1000f) / range)));
        return new TimeshiftBarUiModel(
                host.statusLabel(state),
                progress,
                state.liveCapable,
                () -> {
                    host.showControls();
                    if (!host.resumeLive()) {
                        host.showUnavailable();
                    }
                    host.update();
                },
                () -> {
                    host.markDragging(true);
                    host.showControls();
                },
                () -> host.markDragging(false),
                previewProgress -> {
                    long previewTarget = state.startMs + Math.round((previewProgress / 1000f) * range);
                    return host.previewLabel(state, previewTarget);
                },
                commitProgress -> {
                    long target = state.startMs + Math.round((commitProgress / 1000f) * range);
                    host.seekTo(target);
                    host.markDragging(false);
                    host.update();
                    host.scheduleAutoHide();
                },
                false
        );
    }
}
