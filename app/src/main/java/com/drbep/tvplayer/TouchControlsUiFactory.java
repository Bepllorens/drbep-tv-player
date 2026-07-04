package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class TouchControlsUiFactory {
    interface Host {
        String text(int resId);
        String currentFilterLabel();
        ChannelItem currentChannel();
        boolean isOverlayVisible();
        boolean isTabletOrientationLocked();
        void keepVisible();
        void hideOverlay();
        void showOverlay();
        void showFilterPicker();
        void showVodLibrary();
        void openTimelineGuide();
        void showVodInfo(ChannelItem item);
        void tunePreviousChannel();
        void openProgramInfo();
        void showPlaybackDiagnostics();
        void openRecordings();
        void showToolsMenu();
        void toggleTabletOrientationLock();
        boolean seekBack();
        boolean seekForward();
        void showSeekUnavailable();
        void togglePlayback();
    }

    private TouchControlsUiFactory() {
    }

    static TouchControlsBarUiModel build(Host host) {
        if (host == null) {
            return new TouchControlsBarUiModel(new ArrayList<>());
        }
        ChannelItem current = host.currentChannel();
        boolean vod = current != null && current.isVod;
        List<ZapActionItem> actions = new ArrayList<>();
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_list),
                true,
                false,
                host.isOverlayVisible(),
                () -> {
                    host.keepVisible();
                    if (host.isOverlayVisible()) {
                        host.hideOverlay();
                    } else {
                        host.showOverlay();
                    }
                }
        ));
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_platform),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    host.showFilterPicker();
                }
        ));
        actions.add(new ZapActionItem(
                host.text(vod ? R.string.touch_button_vod_library : R.string.touch_button_guide),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    if (vod) {
                        host.showVodLibrary();
                    } else {
                        host.openTimelineGuide();
                    }
                }
        ));
        actions.add(new ZapActionItem(
                host.text(vod ? R.string.touch_button_vod_detail : R.string.touch_button_previous),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    if (vod && current != null) {
                        host.showVodInfo(current);
                    } else {
                        host.tunePreviousChannel();
                    }
                }
        ));
        actions.add(new ZapActionItem(
                host.text(vod ? R.string.touch_button_vod_detail : R.string.touch_button_info),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    host.openProgramInfo();
                },
                () -> {
                    host.keepVisible();
                    host.showPlaybackDiagnostics();
                }
        ));
        if (vod) {
            actions.add(new ZapActionItem(
                    host.text(R.string.touch_button_vod_library),
                    true,
                    false,
                    false,
                    () -> {
                        host.keepVisible();
                        host.showVodLibrary();
                    }
            ));
        }
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_recordings),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    host.openRecordings();
                },
                () -> {
                    host.keepVisible();
                    host.showToolsMenu();
                }
        ));
        actions.add(new ZapActionItem(
                host.text(host.isTabletOrientationLocked() ? R.string.touch_button_rotate_locked : R.string.touch_button_rotate_free),
                true,
                false,
                host.isTabletOrientationLocked(),
                () -> {
                    host.keepVisible();
                    host.toggleTabletOrientationLock();
                }
        ));
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_rewind),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    if (!host.seekBack()) {
                        host.showSeekUnavailable();
                    }
                }
        ));
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_play_pause),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    host.togglePlayback();
                }
        ));
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_forward),
                true,
                false,
                false,
                () -> {
                    host.keepVisible();
                    if (!host.seekForward()) {
                        host.showSeekUnavailable();
                    }
                }
        ));
        return new TouchControlsBarUiModel(
                host.text(R.string.filter_navigation_hint),
                host.currentFilterLabel(),
                () -> {
                    host.keepVisible();
                    host.showFilterPicker();
                },
                actions
        );
    }
}
