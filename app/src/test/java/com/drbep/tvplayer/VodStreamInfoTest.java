package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class VodStreamInfoTest {
    @Test public void cropped4kKeepsExactResolutionAndDeclaredBitrate() {
        assertEquals("4K · 3840 × 1608 · HDR · PQ · HEVC · 18,5 Mb/s",
                VodStreamInfo.label(3840, 1608, "video/hevc", "hvc1", "HDR · PQ", 18500000));
    }
    @Test public void unknownFieldsAreNotInvented() {
        assertEquals("", VodStreamInfo.label(-1, -1, null, null, "", -1));
        assertEquals("Full HD · 1920 × 800 · H.264",
                VodStreamInfo.label(1920, 800, "video/avc", "avc1", "", -1));
    }
    @Test public void newActiveFormatDoesNotReuseOldBitrateOrHdr() {
        VodStreamInfo.label(3840, 2160, "video/hevc", "", "HDR", 20000000);
        assertEquals("HD · 1280 × 720 · AV1", VodStreamInfo.label(1280, 720, "video/av01", "", "", -1));
    }
}
