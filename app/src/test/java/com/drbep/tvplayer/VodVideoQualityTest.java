package com.drbep.tvplayer;
import androidx.media3.common.Format;
import org.junit.Test;
import static org.junit.Assert.*;

public class VodVideoQualityTest {
    @Test public void croppedImageKeepsActualDimensions() {
        String label=VodVideoQuality.label(new Format.Builder().setWidth(1918).setHeight(802).setCodecs("avc1.640028").setAverageBitrate(6000000).build());
        assertTrue(label.contains("1918 × 802"));assertFalse(label.contains("1080p"));assertTrue(label.contains("SDR"));
    }
    @Test public void visionVariantIsNotMergedWithSdr() {
        Format sdr=new Format.Builder().setWidth(3840).setHeight(1608).setCodecs("hvc1.2.4").build();
        Format vision=sdr.buildUpon().setCodecs("dvh1.05.06").build();
        assertNotEquals(VodVideoQuality.key(sdr),VodVideoQuality.key(vision));
        assertTrue(VodVideoQuality.label(vision).contains("DOLBY VISION"));
    }
    @Test public void absentBitrateIsNotInvented() {
        assertFalse(VodVideoQuality.label(new Format.Builder().build()).contains("Mb/s"));
    }
}
