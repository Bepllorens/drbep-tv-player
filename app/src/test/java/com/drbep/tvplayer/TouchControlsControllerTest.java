package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TouchControlsControllerTest {
    @Test
    public void showTouchControlsSchedulesAutoHideOnTouchDevices() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeHost host = new FakeHost();
        host.touchDeviceMode = true;
        TouchControlsController controller = new TouchControlsController(scheduler, host, 3000L, 3500L);

        controller.showTouchControlsTemporarily();

        assertTrue(host.touchControlsVisible);
        assertEquals(1, host.homeUpdates);
        assertEquals(2, host.timeshiftUpdates);
        assertEquals(3000L, scheduler.lastDelayMs);
    }

    @Test
    public void autoHideHidesTouchControlsHomeAndTimeshift() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeHost host = new FakeHost();
        host.touchDeviceMode = true;
        host.touchControlsVisible = true;
        TouchControlsController controller = new TouchControlsController(scheduler, host, 3000L, 3500L);

        controller.scheduleTouchControlsAutoHide();
        scheduler.runLast();

        assertFalse(host.touchControlsVisible);
        assertTrue(host.homeHidden);
        assertTrue(host.timeshiftHidden);
    }

    @Test
    public void showTouchControlsAlsoSchedulesAutoHideOnTvDevices() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeHost host = new FakeHost();
        host.touchDeviceMode = false;
        TouchControlsController controller = new TouchControlsController(scheduler, host, 3000L, 3500L);

        controller.showTouchControlsTemporarily();

        assertTrue(host.touchControlsVisible);
        assertEquals(3000L, scheduler.lastDelayMs);

        scheduler.runLast();

        assertFalse(host.touchControlsVisible);
        assertTrue(host.timeshiftHidden);
    }

    @Test
    public void showTimeshiftHudRequiresTvModeAndSeekablePlayback() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeHost host = new FakeHost();
        host.touchDeviceMode = false;
        host.seekablePlayback = true;
        TouchControlsController controller = new TouchControlsController(scheduler, host, 3000L, 3500L);

        controller.showTimeshiftHudTemporarily();

        assertTrue(controller.isTvTimeshiftHudVisible());
        assertTrue(controller.isTvTimeshiftHudActive());
        assertEquals(3500L, scheduler.lastDelayMs);
    }

    @Test
    public void overlayMakesTimeshiftHudInactive() {
        FakeScheduler scheduler = new FakeScheduler();
        FakeHost host = new FakeHost();
        host.touchDeviceMode = false;
        host.seekablePlayback = true;
        TouchControlsController controller = new TouchControlsController(scheduler, host, 3000L, 3500L);

        controller.showTimeshiftHudTemporarily();
        host.overlayVisible = true;

        assertTrue(controller.isTvTimeshiftHudVisible());
        assertFalse(controller.isTvTimeshiftHudActive());
    }

    private static final class FakeScheduler implements TouchControlsController.Scheduler {
        Runnable lastRunnable;
        long lastDelayMs;

        @Override
        public void removeCallbacks(Runnable runnable) {
            if (lastRunnable == runnable) {
                lastRunnable = null;
            }
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMs) {
            lastRunnable = runnable;
            lastDelayMs = delayMs;
        }

        void runLast() {
            if (lastRunnable != null) {
                Runnable runnable = lastRunnable;
                lastRunnable = null;
                runnable.run();
            }
        }
    }

    private static final class FakeHost implements TouchControlsController.Host {
        boolean touchDeviceMode;
        boolean touchControlsVisible;
        boolean overlayVisible;
        boolean recordingsVisible;
        boolean multiViewVisible;
        boolean seekablePlayback;
        boolean homeHidden;
        boolean timeshiftHidden;
        int homeUpdates;
        int timeshiftUpdates;

        @Override
        public boolean isTouchDeviceMode() {
            return touchDeviceMode;
        }

        @Override
        public boolean isTouchControlsVisible() {
            return touchControlsVisible;
        }

        @Override
        public boolean isOverlayVisible() {
            return overlayVisible;
        }

        @Override
        public boolean isRecordingsPanelVisible() {
            return recordingsVisible;
        }

        @Override
        public boolean isMultiViewVisible() {
            return multiViewVisible;
        }

        @Override
        public boolean hasSeekablePlayback() {
            return seekablePlayback;
        }

        @Override
        public void setTouchControlsVisible(boolean visible) {
            touchControlsVisible = visible;
        }

        @Override
        public void hideTouchHomeHub() {
            homeHidden = true;
        }

        @Override
        public void hideTimeshiftBar() {
            timeshiftHidden = true;
        }

        @Override
        public void updateTouchHomeHub() {
            homeUpdates++;
        }

        @Override
        public void updateTimeshiftBar() {
            timeshiftUpdates++;
        }
    }
}
