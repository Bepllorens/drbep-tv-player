package com.drbep.tvplayer;

import androidx.media3.common.PlaybackException;

import java.util.Locale;

final class VlcFallbackPolicy {
    static final String PLEX_AVI_PROFILE = "plex-avi-direct";

    private VlcFallbackPolicy() {
    }

    static boolean shouldUseVlc(PlayerController.PlaybackRequest request) {
        if (request == null) {
            return false;
        }
        if (request.vod && request.directPlayback && PLEX_AVI_PROFILE.equals(request.playbackProfile)) {
            return true;
        }
        return false;
    }

    static boolean shouldRetryUnsupportedAudioWithVlc(int errorCode, String message, boolean alreadyAttempted) {
        if (alreadyAttempted || errorCode != PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
            return false;
        }
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return lower.contains("audioc")
                || lower.contains("audio renderer")
                || lower.contains("mediacodecaudiorenderer")
                || lower.contains("audio/mpeg-l2");
    }

    static boolean isPublicHttpsTarget(String targetUrl) {
        String lower = targetUrl == null ? "" : targetUrl.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://");
    }
}
