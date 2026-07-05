package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class PlaybackModeUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String[] modeOptions();
        String currentPermanentMode(ChannelItem item);
        String currentTemporaryMode(ChannelItem item);
        void setPermanentMode(ChannelItem item, String mode, String label);
        void setTemporaryMode(ChannelItem item, String mode, String label);
    }

    private PlaybackModeUiFactory() {
    }

    static TvOptionsMenuModel buildPermanent(ChannelItem item, Host host) {
        return build(item, host.currentPermanentMode(item), host, true);
    }

    static TvOptionsMenuModel buildTemporary(ChannelItem item, Host host) {
        return build(item, host.currentTemporaryMode(item), host, false);
    }

    private static TvOptionsMenuModel build(ChannelItem item, String currentMode, Host host, boolean permanent) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (item == null || host == null) {
            return new TvOptionsMenuModel(labels, actions, "");
        }
        String selectedCurrentMode = sanitizeMode(currentMode);
        int checkedItem = PlaybackModeStore.MODE_DIRECT.equals(selectedCurrentMode)
                ? 1
                : (PlaybackModeStore.MODE_PROXY.equals(selectedCurrentMode) ? 2 : 0);
        String[] options = host.modeOptions();
        for (int i = 0; i < options.length; i++) {
            final int which = i;
            labels.add(i == checkedItem ? host.text(R.string.settings_selected_prefix, options[i]) : options[i]);
            actions.add(() -> {
                String selectedMode = which == 1
                        ? PlaybackModeStore.MODE_DIRECT
                        : (which == 2 ? PlaybackModeStore.MODE_PROXY : PlaybackModeStore.MODE_AUTO);
                if (permanent) {
                    host.setPermanentMode(item, selectedMode, options[which]);
                } else {
                    host.setTemporaryMode(item, selectedMode, options[which]);
                }
            });
        }
        return new TvOptionsMenuModel(labels, actions);
    }

    private static String sanitizeMode(String value) {
        String mode = value == null ? "" : value.trim();
        if (PlaybackModeStore.MODE_DIRECT.equals(mode) || PlaybackModeStore.MODE_PROXY.equals(mode)) {
            return mode;
        }
        return PlaybackModeStore.MODE_AUTO;
    }
}
