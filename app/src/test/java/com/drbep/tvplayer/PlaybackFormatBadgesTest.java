package com.drbep.tvplayer;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PlaybackFormatBadgesTest {
    @Test public void visionDoesNotRequireColorMetadata() {
        assertEquals("DOLBY VISION", PlaybackFormatBadges.video(new Format.Builder().setSampleMimeType("video/dolby-vision").build()));
        assertEquals("DOLBY VISION", PlaybackFormatBadges.video(new Format.Builder().setCodecs("dvh1.05.06").build()));
    }
    @Test public void sdrAndUnknownAreNotHdr() {
        assertEquals("", PlaybackFormatBadges.label(null,null));
        assertEquals("", PlaybackFormatBadges.video(new Format.Builder().setSampleMimeType("video/hevc").build()));
    }
    @Test public void pqAndHlgAreDistinct() {
        assertEquals("HDR · PQ", PlaybackFormatBadges.video(new Format.Builder().setColorInfo(new ColorInfo.Builder().setColorTransfer(C.COLOR_TRANSFER_ST2084).build()).build()));
        assertEquals("HDR · HLG", PlaybackFormatBadges.video(new Format.Builder().setColorInfo(new ColorInfo.Builder().setColorTransfer(C.COLOR_TRANSFER_HLG).build()).build()));
    }
    @Test public void onlyJocProvesAtmos() {
        assertEquals("DOLBY ATMOS", PlaybackFormatBadges.audio(new Format.Builder().setSampleMimeType("audio/eac3-joc").build()));
        assertEquals("DOLBY DIGITAL PLUS", PlaybackFormatBadges.audio(new Format.Builder().setSampleMimeType("audio/eac3").setChannelCount(6).build()));
        assertEquals("DOLBY TRUEHD", PlaybackFormatBadges.audio(new Format.Builder().setSampleMimeType("audio/true-hd").build()));
    }
}
