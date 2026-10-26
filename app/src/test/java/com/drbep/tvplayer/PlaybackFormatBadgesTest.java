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
    @Test public void detailsDescribeEncodedFactsWithoutInventingOutputLayout() {
        Format video = new Format.Builder().setWidth(1920).setHeight(1080).setSampleMimeType("video/avc").setContainerMimeType("video/mp2t").build();
        Format audio = new Format.Builder().setSampleMimeType("audio/eac3").setChannelCount(6).build();
        String details = PlaybackFormatBadges.details(video,audio);
        org.junit.Assert.assertTrue(details.contains("1920 × 1080"));
        org.junit.Assert.assertTrue(details.contains("H.264"));
        org.junit.Assert.assertTrue(details.contains("6 canales"));
        org.junit.Assert.assertTrue(details.contains("TS"));
        org.junit.Assert.assertFalse(details.contains("5.1"));
        org.junit.Assert.assertFalse(details.contains("ATMOS"));
    }
    @Test public void unknownFactsAreOmittedAndAudioOnlyIsSupported() {
        assertEquals("",PlaybackFormatBadges.details(null,null));
        assertEquals("",PlaybackFormatBadges.details(new Format.Builder().build(),null));
        assertEquals("AAC · 2 canales · MP4",PlaybackFormatBadges.details(null,
                new Format.Builder().setSampleMimeType("audio/mp4a-latm").setChannelCount(2).setContainerMimeType("audio/mp4").build()));
    }
}
