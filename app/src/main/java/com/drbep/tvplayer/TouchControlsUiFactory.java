package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class TouchControlsUiFactory {
    interface Host {
        String text(int resId);
        String currentFilterLabel();
        ChannelItem currentChannel();
        default String currentFilterMark() {
            return platformMark(currentChannel());
        }
        default String currentFilterLogoUrl() {
            return platformLogo(currentChannel());
        }
        boolean isOverlayVisible();
        boolean isTabletOrientationLocked();
        boolean supportsOrientationLock();
        void keepVisible();
        void hideOverlay();
        void showOverlay();
        void showFilterPicker();
        void showVodLibrary();
        void openTimelineGuide();
        boolean supportsU7d(ChannelItem item);
        void openU7d(ChannelItem item);
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
        return build(host, 0);
    }

    static TouchControlsBarUiModel build(Host host, int focusedActionIndex) {
        return build(host, focusedActionIndex, TouchControlsNowPlayingUiModel.EMPTY);
    }

    static TouchControlsBarUiModel build(Host host, int focusedActionIndex, TouchControlsNowPlayingUiModel nowPlaying) {
        if (host == null) {
            return new TouchControlsBarUiModel(new ArrayList<>());
        }
        ChannelItem current = host.currentChannel();
        boolean u7dReplay = current != null && "u7d_proxy".equalsIgnoreCase(current.playbackProfile);
        boolean vod = current != null && current.isVod && !u7dReplay;
        List<ZapActionItem> actions = new ArrayList<>();
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_list),
                true,
                false,
                host.isOverlayVisible(),
                () -> {
                    host.keepVisible();
                    host.showOverlay();
                }
        ));
        actions.add(new ZapActionItem(
                host.text(R.string.touch_button_platform),
                true,
                false,
                false,
                "platform",
                host.currentFilterMark(),
                host.currentFilterLogoUrl(),
                () -> {
                    host.keepVisible();
                    host.showFilterPicker();
                },
                null
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
        if (!vod && host.supportsU7d(current)) {
            actions.add(new ZapActionItem(
                    host.text(R.string.touch_button_u7d),
                    true,
                    false,
                    false,
                    () -> {
                        host.keepVisible();
                        host.openU7d(current);
                    }
            ));
        }
        if (!vod) {
            actions.add(new ZapActionItem(
                    host.text(R.string.touch_button_vod),
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
        if (!vod) {
            actions.add(new ZapActionItem(
                    host.text(R.string.touch_button_info),
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
        if (host.supportsOrientationLock()) {
            actions.add(new ZapActionItem(
                    host.text(R.string.touch_button_settings),
                    true,
                    false,
                    false,
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
        }
        if (vod || u7dReplay) {
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
        }
        return new TouchControlsBarUiModel(
                host.text(R.string.filter_navigation_hint),
                host.currentFilterLabel(),
                () -> {
                    host.keepVisible();
                    host.showFilterPicker();
                },
                actions,
                focusedActionIndex,
                nowPlaying
        );
    }

    static String platformMark(ChannelItem channel) {
        String platform = channel == null || channel.platformName == null
                ? ""
                : channel.platformName.trim();
        return compactMark(platform);
    }

    static String compactMark(String label) {
        String value = label == null ? "" : label.trim();
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("movistar")) return "M+";
        if (lower.contains("dazn")) return "DAZN";
        if (lower.contains("tivify")) return "TIV";
        if (lower.contains("plex")) return "PLEX";
        if (lower.contains("orange")) return "ORA";
        String compact = value.replaceAll("[^\\p{L}\\p{N}+]", "").toUpperCase(java.util.Locale.ROOT);
        if (compact.isEmpty()) return "OTT";
        return compact.substring(0, Math.min(4, compact.length()));
    }

    static String platformLogo(ChannelItem channel) {
        return channel == null || channel.platformLogoUrl == null
                ? ""
                : channel.platformLogoUrl.trim();
    }
}
