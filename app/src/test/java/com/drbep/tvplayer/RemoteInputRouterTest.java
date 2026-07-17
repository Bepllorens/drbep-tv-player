package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;

import org.junit.Test;

public class RemoteInputRouterTest {
    @Test
    public void lettersOpenQuickSearchWhenRecordingsPanelIsClosed() {
        FakeHost host = new FakeHost();
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_UNKNOWN, 'a', 0));

        assertEquals("quick:a", host.lastAction);
    }

    @Test
    public void dpadRightTunesQuickSearchWhenQuickSearchIsVisible() {
        FakeHost host = new FakeHost();
        host.quickSearchVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0));

        assertEquals("quick:tune", host.lastAction);
    }

    @Test
    public void backClosesRecordingsPanelBeforeLeavingPlayback() {
        FakeHost host = new FakeHost();
        host.recordingsVisible = true;
        host.playingRecordingWithReturnTarget = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0));

        assertEquals("recordings:hide", host.lastAction);
    }

    @Test
    public void dpadLeftRightMoveRecordingsHeaderFocus() {
        FakeHost host = new FakeHost();
        host.recordingsVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, 0));
        assertEquals("recordings:header:-1", host.lastAction);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0));
        assertEquals("recordings:header:1", host.lastAction);
    }

    @Test
    public void confirmOnOverlayTunesSelectionAndHidesOverlay() {
        FakeHost host = new FakeHost();
        host.overlayVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0, 0));

        assertEquals("overlay:tune-hide", host.lastAction);
    }

    @Test
    public void confirmOnTvSeekablePlaybackShowsBottomControls() {
        FakeHost host = new FakeHost();
        host.seekablePlayback = true;
        host.touchDeviceMode = false;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0, 0));

        assertEquals("controls:show", host.lastAction);
    }

    @Test
    public void repeatedFastForwardJumpsToLiveWhenPossible() {
        FakeHost host = new FakeHost();
        host.resumeTimeshiftLive = true;
        host.seekTimeshiftForward = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 0, 1));

        assertEquals("timeshift:show", host.lastAction);
        assertEquals(1, host.resumeLiveCalls);
        assertEquals(0, host.seekForwardCalls);
    }

    @Test
    public void multiViewConsumesDpadBeforeNormalNavigation() {
        FakeHost host = new FakeHost();
        host.multiViewVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, 0));

        assertEquals("multi:move:-1,0", host.lastAction);
    }

    @Test
    public void touchControlsVisibleConsumesDpadNavigation() {
        FakeHost host = new FakeHost();
        host.touchControlsVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0));
        assertEquals("controls:move:1", host.lastAction);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, 0));
        assertEquals("controls:timeshift", host.lastAction);
        assertEquals(0, host.tuneCalls);
    }

    @Test
    public void touchControlsTimeshiftFocusConsumesSeekKeys() {
        FakeHost host = new FakeHost();
        host.touchControlsVisible = true;
        host.touchControlsTimeshiftFocused = true;
        host.seekTimeshiftForward = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, 0));

        assertEquals("timeshift:show", host.lastAction);
        assertEquals(1, host.seekForwardCalls);
    }

    @Test
    public void touchControlsDownReturnsToActionsFromTimeshift() {
        FakeHost host = new FakeHost();
        host.touchControlsVisible = true;
        host.touchControlsTimeshiftFocused = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, 0));

        assertEquals("controls:actions", host.lastAction);
        assertEquals(0, host.tuneCalls);
    }

    @Test
    public void confirmActivatesVisibleTouchControls() {
        FakeHost host = new FakeHost();
        host.touchControlsVisible = true;
        RemoteInputRouter router = new RemoteInputRouter(host, 450L);

        assertTrue(router.dispatchKey(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0, 0));

        assertEquals("controls:activate", host.lastAction);
    }

    private static final class FakeHost implements RemoteInputRouter.Host {
        boolean recordingsVisible;
        boolean quickSearchVisible;
        boolean multiViewVisible;
        boolean overlayVisible;
        boolean tvTimeshiftHudActive;
        boolean resumeTimeshiftLive;
        boolean seekTimeshiftBack;
        boolean seekTimeshiftForward;
        boolean playingRecordingWithReturnTarget;
        boolean seekablePlayback;
        boolean touchDeviceMode;
        boolean touchControlsVisible;
        boolean touchControlsTimeshiftFocused;
        boolean selectedOverlayChannel = true;
        boolean currentChannel = true;
        int resumeLiveCalls;
        int seekForwardCalls;
        int tuneCalls;
        String lastAction = "";

        @Override
        public boolean isRecordingsPanelVisible() {
            return recordingsVisible;
        }

        @Override
        public boolean isQuickSearchVisible() {
            return quickSearchVisible;
        }

        @Override
        public boolean isMultiViewVisible() {
            return multiViewVisible;
        }

        @Override
        public boolean isOverlayVisible() {
            return overlayVisible;
        }

        @Override
        public boolean isZapBannerVisible() {
            return false;
        }

        @Override
        public boolean isTvTimeshiftHudActive() {
            return tvTimeshiftHudActive;
        }

        @Override
        public boolean isTouchControlsVisible() {
            return touchControlsVisible;
        }

        @Override
        public boolean isTouchControlsTimeshiftFocused() {
            return touchControlsTimeshiftFocused;
        }

        @Override
        public boolean canResumeTimeshiftLive() {
            return resumeTimeshiftLive;
        }

        @Override
        public boolean resumeTimeshiftLive() {
            resumeLiveCalls++;
            lastAction = "timeshift:live";
            return resumeTimeshiftLive;
        }

        @Override
        public boolean canSeekTimeshiftBack() {
            return seekTimeshiftBack;
        }

        @Override
        public boolean canSeekTimeshiftForward() {
            seekForwardCalls++;
            return seekTimeshiftForward;
        }

        @Override
        public boolean seekTimeshiftBack() {
            lastAction = "timeshift:back";
            return seekTimeshiftBack;
        }

        @Override
        public boolean seekTimeshiftForward() {
            seekForwardCalls++;
            lastAction = "timeshift:forward";
            return seekTimeshiftForward;
        }

        @Override
        public boolean isPlayingRecordingWithReturnTarget() {
            return playingRecordingWithReturnTarget;
        }

        @Override
        public boolean hasSeekablePlayback() {
            return seekablePlayback;
        }

        @Override
        public boolean isTouchDeviceMode() {
            return touchDeviceMode;
        }

        @Override
        public boolean hasSelectedOverlayChannel() {
            return selectedOverlayChannel;
        }

        @Override
        public boolean hasCurrentChannel() {
            return currentChannel;
        }

        @Override
        public int getMultiViewActiveIndex() {
            return 0;
        }

        @Override
        public void handleQuickSearchCharacter(char value) {
            lastAction = "quick:" + value;
        }

        @Override
        public void clearQuickSearchOverlay() {
            lastAction = "quick:clear";
        }

        @Override
        public void switchRecordingsMode(boolean scheduledMode) {
            lastAction = "recordings:mode:" + scheduledMode;
        }

        @Override
        public void moveRecordingsHeaderFocus(int delta) {
            lastAction = "recordings:header:" + delta;
        }

        @Override
        public boolean activateRecordingsHeaderFocus() {
            lastAction = "recordings:header:activate";
            return false;
        }

        @Override
        public void showChannelActionMenu() {
            lastAction = "channel:actions";
        }

        @Override
        public void openTimelineGuideAroundSelection() {
            lastAction = "timeline:selected";
        }

        @Override
        public void openTimelineGuideForCurrentChannel() {
            lastAction = "timeline:current";
        }

        @Override
        public void showOverlay() {
            lastAction = "overlay:show";
        }

        @Override
        public void showV12ToolsMenu() {
            lastAction = "tools";
        }

        @Override
        public void hideRecordingsPanel() {
            lastAction = "recordings:hide";
        }

        @Override
        public void hideTvTimeshiftHud() {
            lastAction = "timeshift:hide";
        }

        @Override
        public void hideTouchControls() {
            lastAction = "controls:hide";
        }

        @Override
        public void moveTouchControlsFocus(int delta) {
            lastAction = "controls:move:" + delta;
        }

        @Override
        public void focusTouchControlsTimeshift() {
            lastAction = "controls:timeshift";
        }

        @Override
        public void focusTouchControlsActions() {
            lastAction = "controls:actions";
        }

        @Override
        public void activateTouchControlsFocus() {
            lastAction = "controls:activate";
        }

        @Override
        public void showLeaveRecordingPrompt() {
            lastAction = "recording:leave";
        }

        @Override
        public void hideOverlay() {
            lastAction = "overlay:hide";
        }

        @Override
        public void hideZapBanner() {
            lastAction = "zap:hide";
        }

        @Override
        public void moveZapBannerSelection(int delta) {
            lastAction = "zap:move:" + delta;
        }

        @Override
        public void activateZapBannerSelection() {
            lastAction = "zap:activate";
        }

        @Override
        public void finishActivity() {
            lastAction = "finish";
        }

        @Override
        public void moveQuickSearchSelection(int delta) {
            lastAction = "quick:move:" + delta;
        }

        @Override
        public void moveRecordingsSelection(int delta) {
            lastAction = "recordings:move:" + delta;
        }

        @Override
        public void tuneRelative(int delta) {
            tuneCalls++;
            lastAction = "tune:" + delta;
        }

        @Override
        public void showTouchControlsTemporarily() {
            lastAction = "controls:show";
        }

        @Override
        public void showTimeshiftHudTemporarily() {
            lastAction = "timeshift:show";
        }

        @Override
        public void moveOverlaySelection(int delta) {
            lastAction = "overlay:move:" + delta;
        }

        @Override
        public void cycleFilter(int delta) {
            lastAction = "filter:" + delta;
        }

        @Override
        public void tuneQuickSearchSelection() {
            lastAction = "quick:tune";
        }

        @Override
        public void playSelectedRecording() {
            lastAction = "recordings:play";
        }

        @Override
        public void tuneOverlaySelectionAndHide() {
            lastAction = "overlay:tune-hide";
        }

        @Override
        public void togglePlayback() {
            lastAction = "playback:toggle";
        }

        @Override
        public void showChannelSearchDialog() {
            lastAction = "search:dialog";
        }

        @Override
        public void showRecordingActionsDialog() {
            lastAction = "recordings:actions";
        }

        @Override
        public void deleteQuickSearchCharacter() {
            lastAction = "quick:delete";
        }

        @Override
        public void scheduleSelectedOrCurrentProgram() {
            lastAction = "record:schedule";
        }

        @Override
        public void closeMultiView() {
            lastAction = "multi:close";
        }

        @Override
        public void moveMultiViewSelection(int columnDelta, int rowDelta) {
            lastAction = "multi:move:" + columnDelta + "," + rowDelta;
        }

        @Override
        public void focusMultiViewSlot(int slot) {
            lastAction = "multi:focus:" + slot;
        }

        @Override
        public void showMultiViewChannelPicker(int slot) {
            lastAction = "multi:picker:" + slot;
        }
    }
}
