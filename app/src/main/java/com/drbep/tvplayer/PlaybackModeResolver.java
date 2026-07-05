package com.drbep.tvplayer;

import java.util.Map;

final class PlaybackModeResolver {
    interface Host {
        boolean standaloneMode();
        boolean playbackRepairEnabled();
        boolean proxyManifestProfile(ChannelItem item);
        void saveLearnedModes();
    }

    private PlaybackModeResolver() {
    }

    static String resolve(ChannelItem item, PlaybackModeStore store, Map<String, String> temporaryModes, Map<String, String> learnedModes, Host host) {
        if (item == null || item.id == null) {
            return PlaybackModeStore.MODE_AUTO;
        }
        if (host != null && !host.standaloneMode() && host.proxyManifestProfile(item)) {
            boolean changed = false;
            if (PlaybackModeStore.MODE_DIRECT.equals(sanitizeMode(temporaryModes.get(item.id)))) {
                temporaryModes.remove(item.id);
            }
            if (PlaybackModeStore.MODE_DIRECT.equals(sanitizeMode(learnedModes.get(item.id)))) {
                learnedModes.remove(item.id);
                changed = true;
            }
            if (changed) {
                host.saveLearnedModes();
            }
        }
        if (temporaryModes.containsKey(item.id)) {
            return sanitizeMode(temporaryModes.get(item.id));
        }
        String permanentMode = store == null ? PlaybackModeStore.MODE_AUTO : store.getMode(item.id);
        if (!PlaybackModeStore.MODE_AUTO.equals(permanentMode)) {
            return permanentMode;
        }
        if (host == null || !host.playbackRepairEnabled()) {
            return PlaybackModeStore.MODE_AUTO;
        }
        String learnedMode = sanitizeMode(learnedModes.get(item.id));
        if (host.standaloneMode() && PlaybackModeStore.MODE_PROXY.equals(learnedMode)) {
            return PlaybackModeStore.MODE_AUTO;
        }
        return learnedMode;
    }

    private static String sanitizeMode(String value) {
        String mode = value == null ? "" : value.trim();
        if (PlaybackModeStore.MODE_DIRECT.equals(mode) || PlaybackModeStore.MODE_PROXY.equals(mode)) {
            return mode;
        }
        return PlaybackModeStore.MODE_AUTO;
    }
}
