package com.drbep.tvplayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class PlaybackRecoveryCoordinator {
    private final Map<String, String> temporaryModes;
    private final Map<String, String> learnedModes;
    private final Map<String, Set<String>> repairAttempts;

    PlaybackRecoveryCoordinator(Map<String, String> temporaryModes, Map<String, String> learnedModes, Map<String, Set<String>> repairAttempts) {
        this.temporaryModes = temporaryModes;
        this.learnedModes = learnedModes;
        this.repairAttempts = repairAttempts;
    }

    String nextMode(String currentMode) {
        String clean = sanitizeMode(currentMode);
        if (PlaybackModeStore.MODE_AUTO.equals(clean)) {
            return PlaybackModeStore.MODE_DIRECT;
        }
        if (PlaybackModeStore.MODE_DIRECT.equals(clean)) {
            return PlaybackModeStore.MODE_PROXY;
        }
        return PlaybackModeStore.MODE_AUTO;
    }

    void setTemporaryMode(String channelId, String mode) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        String clean = sanitizeMode(mode);
        if (PlaybackModeStore.MODE_AUTO.equals(clean)) {
            temporaryModes.remove(channelId);
        } else {
            temporaryModes.put(channelId, clean);
        }
    }

    void clearTemporaryMode(String channelId) {
        if (channelId != null) {
            temporaryModes.remove(channelId);
        }
    }

    boolean clearLearnedMode(String channelId) {
        return channelId != null && learnedModes.remove(channelId) != null;
    }

    void setLearnedMode(String channelId, String mode) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        String clean = sanitizeMode(mode);
        if (PlaybackModeStore.MODE_AUTO.equals(clean)) {
            learnedModes.remove(channelId);
        } else {
            learnedModes.put(channelId, clean);
        }
    }

    String learnedMode(String channelId) {
        return sanitizeMode(channelId == null ? null : learnedModes.get(channelId));
    }

    void clearAttempts(String channelId) {
        if (channelId != null) {
            repairAttempts.remove(channelId);
        }
    }

    boolean markAttempt(String channelId, String mode) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return false;
        }
        String clean = sanitizeMode(mode);
        if (PlaybackModeStore.MODE_AUTO.equals(clean)) {
            return false;
        }
        Set<String> attempts = repairAttempts.get(channelId);
        if (attempts == null) {
            attempts = new HashSet<>();
            repairAttempts.put(channelId, attempts);
        }
        return attempts.add(clean);
    }

    static String sanitizeMode(String value) {
        String mode = value == null ? "" : value.trim();
        if (PlaybackModeStore.MODE_DIRECT.equals(mode)
                || PlaybackModeStore.MODE_PROXY.equals(mode)
                || PlaybackModeStore.MODE_COMPAT.equals(mode)) {
            return mode;
        }
        return PlaybackModeStore.MODE_AUTO;
    }
}
