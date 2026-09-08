package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.PlaybackException;

import org.junit.Test;

public class VlcFallbackPolicyTest {
    @Test
    public void plexAviDirectUsesVlc() {
        PlayerController.PlaybackRequest request = request(true, true, VlcFallbackPolicy.PLEX_AVI_PROFILE);

        assertTrue(VlcFallbackPolicy.shouldUseVlc(request));
    }

    @Test
    public void nonAviAndNonDirectPlaybackStayOnMedia3() {
        assertFalse(VlcFallbackPolicy.shouldUseVlc(request(true, true, "")));
        assertFalse(VlcFallbackPolicy.shouldUseVlc(request(true, false, VlcFallbackPolicy.PLEX_AVI_PROFILE)));
        assertFalse(VlcFallbackPolicy.shouldUseVlc(request(false, true, VlcFallbackPolicy.PLEX_AVI_PROFILE)));
    }

    @Test
    public void movistarSampleAesDirectCopyStaysOnMedia3() {
        PlayerController.PlaybackRequest request = new PlayerController.PlaybackRequest(
                "1113364",
                "LA 2",
                "Movistar HLS",
                "https://iptv.example.com/proxy/manifest/1113364",
                "",
                PlaybackModeStore.MODE_AUTO,
                "",
                "",
                false,
                false,
                "proxy_manifest"
        );

        assertFalse(VlcFallbackPolicy.shouldUseVlc(request));
    }

    @Test
    public void unsupportedMediaCodecAudioRetriesWithVlcOnce() {
        String message = "MediaCodecAudioRenderer error, format=audio/mpeg-L2, format_supported=NO_UNSUPPORTED_SUBTYPE";

        assertTrue(VlcFallbackPolicy.shouldRetryUnsupportedAudioWithVlc(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                message,
                false
        ));
        assertFalse(VlcFallbackPolicy.shouldRetryUnsupportedAudioWithVlc(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                message,
                true
        ));
    }

    @Test
    public void videoDecoderAndNetworkErrorsStayOnMedia3Recovery() {
        assertFalse(VlcFallbackPolicy.shouldRetryUnsupportedAudioWithVlc(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                "MediaCodecVideoRenderer error",
                false
        ));
        assertFalse(VlcFallbackPolicy.shouldRetryUnsupportedAudioWithVlc(
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                "MediaCodecAudioRenderer error",
                false
        ));
    }

    @Test
    public void vlcDecoderFallbackOnlyAcceptsHttpsTargets() {
        assertTrue(VlcFallbackPolicy.isPublicHttpsTarget("https://fire.example.com/proxy/manifest/1"));
        assertFalse(VlcFallbackPolicy.isPublicHttpsTarget("http://adult-proxy:8788/channel/1/index.m3u8"));
        assertFalse(VlcFallbackPolicy.isPublicHttpsTarget(""));
    }

    private static PlayerController.PlaybackRequest request(boolean vod, boolean direct, String profile) {
        return new PlayerController.PlaybackRequest(
                "vod-test",
                "Test",
                "Plex",
                "https://example.test/api/vod/plex/stream/1",
                "",
                PlaybackModeStore.MODE_AUTO,
                "",
                "",
                direct,
                vod,
                profile
        );
    }
}
