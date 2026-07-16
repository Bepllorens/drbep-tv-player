package com.drbep.tvplayer;

import android.view.KeyEvent;

final class RemoteInputRouter {
    interface Host {
        boolean isRecordingsPanelVisible();

        boolean isQuickSearchVisible();

        boolean isMultiViewVisible();

        boolean isOverlayVisible();

        boolean isZapBannerVisible();

        boolean isTvTimeshiftHudActive();

        boolean isTouchControlsVisible();

        boolean isTouchControlsTimeshiftFocused();

        boolean canResumeTimeshiftLive();

        boolean canSeekTimeshiftBack();

        boolean canSeekTimeshiftForward();

        boolean isPlayingRecordingWithReturnTarget();

        boolean hasSeekablePlayback();

        boolean isTouchDeviceMode();

        boolean hasSelectedOverlayChannel();

        boolean hasCurrentChannel();

        int getMultiViewActiveIndex();

        void handleQuickSearchCharacter(char value);

        void clearQuickSearchOverlay();

        void switchRecordingsMode(boolean scheduledMode);

        void moveRecordingsHeaderFocus(int delta);

        boolean activateRecordingsHeaderFocus();

        void showChannelActionMenu();

        void openTimelineGuideAroundSelection();

        void openTimelineGuideForCurrentChannel();

        void showOverlay();

        void showV12ToolsMenu();

        void hideRecordingsPanel();

        void hideTvTimeshiftHud();

        void hideTouchControls();

        void moveTouchControlsFocus(int delta);

        void focusTouchControlsTimeshift();

        void focusTouchControlsActions();

        void activateTouchControlsFocus();

        void showLeaveRecordingPrompt();

        void hideOverlay();

        void hideZapBanner();

        void moveZapBannerSelection(int delta);

        void activateZapBannerSelection();

        void finishActivity();

        void moveQuickSearchSelection(int delta);

        void moveRecordingsSelection(int delta);

        void tuneRelative(int delta);

        void showTouchControlsTemporarily();

        void showTimeshiftHudTemporarily();

        void moveOverlaySelection(int delta);

        void cycleFilter(int delta);

        void tuneQuickSearchSelection();

        void playSelectedRecording();

        void tuneOverlaySelectionAndHide();

        void togglePlayback();

        void showChannelSearchDialog();

        void showRecordingActionsDialog();

        void deleteQuickSearchCharacter();

        void scheduleSelectedOrCurrentProgram();

        void closeMultiView();

        void moveMultiViewSelection(int columnDelta, int rowDelta);

        void focusMultiViewSlot(int slot);

        void showMultiViewChannelPicker(int slot);
    }

    private final Host host;
    private final long menuDoublePressMs;
    private long lastMenuPressedAtMs;

    RemoteInputRouter(Host host, long menuDoublePressMs) {
        this.host = host;
        this.menuDoublePressMs = menuDoublePressMs;
    }

    boolean dispatchKeyEvent(KeyEvent event) {
        if (event == null) {
            return false;
        }
        return dispatchKey(event.getAction(), event.getKeyCode(), event.getUnicodeChar(), event.getRepeatCount());
    }

    boolean dispatchKey(int action, int keyCode, int unicode, int repeatCount) {
        if (action != KeyEvent.ACTION_DOWN) {
            return false;
        }

        if (!host.isRecordingsPanelVisible() && unicode > 0 && Character.isLetterOrDigit((char) unicode)) {
            host.handleQuickSearchCharacter((char) unicode);
            return true;
        }

        if (host.isMultiViewVisible() && handleMultiViewKey(keyCode)) {
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                return handleMenuKey();
            case KeyEvent.KEYCODE_BACK:
                return handleBackKey();
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
                return handleChannelMove(-1);
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                return handleChannelMove(1);
            case KeyEvent.KEYCODE_DPAD_UP:
                return handleDpadUp();
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return handleDpadDown();
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return handleDpadLeft();
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return handleDpadRight();
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return handleConfirmKey();
            case KeyEvent.KEYCODE_INFO:
                return handleInfoKey();
            case KeyEvent.KEYCODE_SEARCH:
                host.showChannelSearchDialog();
                return true;
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
                if (host.isQuickSearchVisible()) {
                    host.deleteQuickSearchCharacter();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_RECORD:
                host.scheduleSelectedOrCurrentProgram();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (host.canSeekTimeshiftBack()) {
                    host.showTimeshiftHudTemporarily();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (repeatCount > 0 && host.canResumeTimeshiftLive()) {
                    host.showTimeshiftHudTemporarily();
                    return true;
                }
                if (host.canSeekTimeshiftForward()) {
                    host.showTimeshiftHudTemporarily();
                    return true;
                }
                return false;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                host.togglePlayback();
                host.showTimeshiftHudTemporarily();
                return true;
            default:
                return false;
        }
    }

    boolean onKeyLongPress(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (host.canResumeTimeshiftLive()) {
                host.showTimeshiftHudTemporarily();
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_INFO || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            host.showV12ToolsMenu();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (host.isQuickSearchVisible()) {
                host.clearQuickSearchOverlay();
                return true;
            }
            if (host.isRecordingsPanelVisible()) {
                host.hideRecordingsPanel();
                return true;
            }
            if (host.isOverlayVisible()) {
                if (host.hasSelectedOverlayChannel()) {
                    host.openTimelineGuideAroundSelection();
                    return true;
                }
            } else if (host.hasCurrentChannel()) {
                host.openTimelineGuideForCurrentChannel();
                return true;
            }
        }
        if (host.isOverlayVisible() && isConfirmKey(keyCode)) {
            host.showChannelActionMenu();
            return true;
        }
        return false;
    }

    private boolean handleMultiViewKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                host.closeMultiView();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                host.moveMultiViewSelection(-1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                host.moveMultiViewSelection(1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                host.moveMultiViewSelection(0, -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                host.moveMultiViewSelection(0, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                host.focusMultiViewSlot(host.getMultiViewActiveIndex());
                return true;
            case KeyEvent.KEYCODE_MENU:
                host.showMultiViewChannelPicker(host.getMultiViewActiveIndex());
                return true;
            default:
                return false;
        }
    }

    private boolean handleMenuKey() {
        long now = System.currentTimeMillis();
        if (host.isQuickSearchVisible()) {
            host.clearQuickSearchOverlay();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.showRecordingActionsDialog();
            return true;
        }
        if (host.isOverlayVisible()) {
            if (now - lastMenuPressedAtMs <= menuDoublePressMs) {
                lastMenuPressedAtMs = 0L;
                if (host.hasSelectedOverlayChannel()) {
                    host.openTimelineGuideAroundSelection();
                }
                return true;
            }
            lastMenuPressedAtMs = now;
            host.showChannelActionMenu();
            return true;
        }
        if (now - lastMenuPressedAtMs <= menuDoublePressMs) {
            lastMenuPressedAtMs = 0L;
            if (host.hasCurrentChannel()) {
                host.openTimelineGuideForCurrentChannel();
            } else {
                host.showOverlay();
            }
            return true;
        }
        lastMenuPressedAtMs = now;
        host.showV12ToolsMenu();
        return true;
    }

    private boolean handleBackKey() {
        if (host.isQuickSearchVisible()) {
            host.clearQuickSearchOverlay();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.hideRecordingsPanel();
            return true;
        }
        if (host.isTouchControlsVisible()) {
            host.hideTouchControls();
            return true;
        }
        if (host.isTvTimeshiftHudActive()) {
            host.hideTvTimeshiftHud();
            return true;
        }
        if (host.isPlayingRecordingWithReturnTarget()) {
            host.showLeaveRecordingPrompt();
            return true;
        }
        if (host.isZapBannerVisible()) {
            host.hideZapBanner();
            return true;
        }
        if (host.isOverlayVisible()) {
            host.hideOverlay();
            return true;
        }
        host.finishActivity();
        return true;
    }

    private boolean handleChannelMove(int delta) {
        if (host.isQuickSearchVisible()) {
            host.moveQuickSearchSelection(delta);
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.moveRecordingsSelection(delta);
            return true;
        }
        host.tuneRelative(delta);
        return true;
    }

    private boolean handleDpadUp() {
        if (host.isQuickSearchVisible()) {
            host.moveQuickSearchSelection(-1);
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.moveRecordingsSelection(-1);
            return true;
        }
        if (host.isTouchControlsVisible()) {
            host.focusTouchControlsTimeshift();
            return true;
        }
        if (host.isTvTimeshiftHudActive()) {
            return true;
        }
        if (host.isOverlayVisible()) {
            host.moveOverlaySelection(-1);
        } else {
            host.tuneRelative(-1);
        }
        return true;
    }

    private boolean handleDpadDown() {
        if (host.isQuickSearchVisible()) {
            host.moveQuickSearchSelection(1);
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.moveRecordingsSelection(1);
            return true;
        }
        if (host.isTouchControlsVisible()) {
            host.focusTouchControlsActions();
            return true;
        }
        if (host.isTvTimeshiftHudActive()) {
            return true;
        }
        if (host.isOverlayVisible()) {
            host.moveOverlaySelection(1);
        } else {
            host.tuneRelative(1);
        }
        return true;
    }

    private boolean handleDpadLeft() {
        if (host.isQuickSearchVisible()) {
            host.clearQuickSearchOverlay();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.moveRecordingsHeaderFocus(-1);
            return true;
        }
        if (host.isTouchControlsVisible()) {
            if (host.isTouchControlsTimeshiftFocused()) {
                if (host.canSeekTimeshiftBack()) {
                    host.focusTouchControlsTimeshift();
                }
                return true;
            }
            host.moveTouchControlsFocus(-1);
            return true;
        }
        if (host.isTvTimeshiftHudActive() && host.canSeekTimeshiftBack()) {
            host.showTimeshiftHudTemporarily();
            return true;
        }
        if (host.isZapBannerVisible()) {
            host.moveZapBannerSelection(-1);
            return true;
        }
        if (host.isOverlayVisible()) {
            host.cycleFilter(-1);
        } else {
            host.showOverlay();
        }
        return true;
    }

    private boolean handleDpadRight() {
        if (host.isQuickSearchVisible()) {
            host.tuneQuickSearchSelection();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.moveRecordingsHeaderFocus(1);
            return true;
        }
        if (host.isTouchControlsVisible()) {
            if (host.isTouchControlsTimeshiftFocused()) {
                if (host.canSeekTimeshiftForward()) {
                    host.focusTouchControlsTimeshift();
                }
                return true;
            }
            host.moveTouchControlsFocus(1);
            return true;
        }
        if (host.isTvTimeshiftHudActive() && host.canSeekTimeshiftForward()) {
            host.showTimeshiftHudTemporarily();
            return true;
        }
        if (host.isZapBannerVisible()) {
            host.moveZapBannerSelection(1);
            return true;
        }
        if (host.isOverlayVisible()) {
            host.cycleFilter(1);
        } else {
            host.showOverlay();
        }
        return true;
    }

    private boolean handleConfirmKey() {
        if (host.isQuickSearchVisible()) {
            host.tuneQuickSearchSelection();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            if (host.activateRecordingsHeaderFocus()) {
                return true;
            }
            host.playSelectedRecording();
            return true;
        }
        if (host.isTouchControlsVisible()) {
            if (host.isTouchControlsTimeshiftFocused()) {
                host.togglePlayback();
                return true;
            }
            host.activateTouchControlsFocus();
            return true;
        }
        if (host.isZapBannerVisible()) {
            host.showTouchControlsTemporarily();
            return true;
        }
        if (host.isOverlayVisible()) {
            host.tuneOverlaySelectionAndHide();
        } else if (host.hasSeekablePlayback()) {
            host.showTouchControlsTemporarily();
        } else {
            host.togglePlayback();
        }
        return true;
    }

    private boolean handleInfoKey() {
        if (host.isQuickSearchVisible()) {
            host.showChannelSearchDialog();
            return true;
        }
        if (host.isRecordingsPanelVisible()) {
            host.showRecordingActionsDialog();
            return true;
        }
        if (host.isOverlayVisible()) {
            if (host.hasSelectedOverlayChannel()) {
                host.openTimelineGuideAroundSelection();
            }
            return true;
        }
        if (host.hasCurrentChannel()) {
            host.openTimelineGuideForCurrentChannel();
        } else {
            host.showOverlay();
        }
        return true;
    }

    private static boolean isConfirmKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }
}
