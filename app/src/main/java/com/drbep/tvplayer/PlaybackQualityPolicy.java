package com.drbep.tvplayer;

import java.util.Locale;

/** Central policy shared by TV, tablet and phone playback surfaces. */
final class PlaybackQualityPolicy {
    static final String AUTO = "auto";
    static final String DATA_SAVER = "data_saver";
    static final String HIGH = "high";

    private PlaybackQualityPolicy() {
    }

    static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (DATA_SAVER.equals(normalized) || HIGH.equals(normalized)) {
            return normalized;
        }
        return AUTO;
    }

    static boolean forceHighestBitrate(String mode) {
        return HIGH.equals(normalize(mode));
    }

    static int maxWidth(String mode, boolean compatibilityCap, boolean multiView) {
        if (multiView) {
            return 960;
        }
        if (compatibilityCap || DATA_SAVER.equals(normalize(mode))) {
            return 1280;
        }
        return Integer.MAX_VALUE;
    }

    static int maxHeight(String mode, boolean compatibilityCap, boolean multiView) {
        if (multiView) {
            return 540;
        }
        if (compatibilityCap || DATA_SAVER.equals(normalize(mode))) {
            return 720;
        }
        return Integer.MAX_VALUE;
    }

    static int maxBitrate(String mode, boolean multiView) {
        if (multiView) {
            return 1_800_000;
        }
        if (DATA_SAVER.equals(normalize(mode))) {
            return 3_000_000;
        }
        return Integer.MAX_VALUE;
    }
}
