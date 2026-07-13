package com.drbep.tvplayer;

import android.os.Handler;

final class TouchControlsController {
    interface Scheduler {
        void removeCallbacks(Runnable runnable);

        void postDelayed(Runnable runnable, long delayMs);
    }

    interface Host {
        boolean isTouchDeviceMode();

        boolean isTouchControlsVisible();

        boolean isOverlayVisible();

        boolean isRecordingsPanelVisible();

        boolean isMultiViewVisible();

        boolean hasSeekablePlayback();

        void setTouchControlsVisible(boolean visible);

        void hideTouchHomeHub();

        void hideTimeshiftBar();

        void updateTouchHomeHub();

        void updateTimeshiftBar();
    }

    private final Scheduler scheduler;
    private final Host host;
    private final long touchControlsHideMs;
    private final long tvTimeshiftHudHideMs;
    private boolean tvTimeshiftHudVisible;

    private final Runnable hideTouchControlsRunnable;
    private final Runnable hideTvTimeshiftHudRunnable;

    TouchControlsController(Handler uiHandler, Host host, long touchControlsHideMs, long tvTimeshiftHudHideMs) {
        this(new HandlerScheduler(uiHandler), host, touchControlsHideMs, tvTimeshiftHudHideMs);
    }

    TouchControlsController(Scheduler scheduler, Host host, long touchControlsHideMs, long tvTimeshiftHudHideMs) {
        this.scheduler = scheduler;
        this.host = host;
        this.touchControlsHideMs = touchControlsHideMs;
        this.tvTimeshiftHudHideMs = tvTimeshiftHudHideMs;
        this.hideTouchControlsRunnable = this::hideTouchControlsFromTimer;
        this.hideTvTimeshiftHudRunnable = () -> {
            tvTimeshiftHudVisible = false;
            host.updateTimeshiftBar();
        };
    }

    boolean isTvTimeshiftHudVisible() {
        return tvTimeshiftHudVisible;
    }

    boolean isTvTimeshiftHudActive() {
        return !host.isTouchDeviceMode()
                && tvTimeshiftHudVisible
                && !host.isOverlayVisible()
                && !host.isRecordingsPanelVisible()
                && !host.isMultiViewVisible();
    }

    void showTouchControlsTemporarily() {
        host.setTouchControlsVisible(true);
        host.updateTouchHomeHub();
        host.updateTimeshiftBar();
        scheduleTouchControlsAutoHide();
    }

    void showTimeshiftHudTemporarily() {
        if (host.isTouchDeviceMode() || !host.hasSeekablePlayback()) {
            return;
        }
        tvTimeshiftHudVisible = true;
        host.updateTimeshiftBar();
        scheduleTvTimeshiftHudAutoHide();
    }

    void hideTvTimeshiftHud() {
        if (host.isTouchDeviceMode() || !tvTimeshiftHudVisible) {
            return;
        }
        scheduler.removeCallbacks(hideTvTimeshiftHudRunnable);
        tvTimeshiftHudVisible = false;
        host.updateTimeshiftBar();
    }

    void scheduleTouchControlsAutoHide() {
        scheduler.removeCallbacks(hideTouchControlsRunnable);
        host.updateTimeshiftBar();
        if (host.isTouchControlsVisible()) {
            scheduler.postDelayed(hideTouchControlsRunnable, touchControlsHideMs);
        }
    }

    void scheduleTvTimeshiftHudAutoHide() {
        scheduler.removeCallbacks(hideTvTimeshiftHudRunnable);
        host.updateTimeshiftBar();
        if (!host.isTouchDeviceMode() && tvTimeshiftHudVisible) {
            scheduler.postDelayed(hideTvTimeshiftHudRunnable, tvTimeshiftHudHideMs);
        }
    }

    void cancelTimers() {
        scheduler.removeCallbacks(hideTouchControlsRunnable);
        scheduler.removeCallbacks(hideTvTimeshiftHudRunnable);
    }

    void hideAllTransientControls() {
        hideTouchControlsFromTimer();
    }

    private void hideTouchControlsFromTimer() {
        if (!host.isOverlayVisible() && !host.isRecordingsPanelVisible()) {
            host.setTouchControlsVisible(false);
            host.hideTouchHomeHub();
        }
        host.hideTouchHomeHub();
        host.hideTimeshiftBar();
        tvTimeshiftHudVisible = false;
        scheduler.removeCallbacks(hideTvTimeshiftHudRunnable);
    }

    private static final class HandlerScheduler implements Scheduler {
        private final Handler handler;

        HandlerScheduler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            if (handler != null) {
                handler.removeCallbacks(runnable);
            }
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            if (handler != null) {
                handler.postDelayed(runnable, delayMs);
            }
        }
    }
}
