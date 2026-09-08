package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PictureInPictureAspectRatioPolicyTest {
    @Test
    public void keepsAndReducesKnownVideoRatios() {
        PictureInPictureAspectRatioPolicy.Ratio widescreen = PictureInPictureAspectRatioPolicy.resolve(1920, 1080);
        assertEquals(16, widescreen.width);
        assertEquals(9, widescreen.height);

        PictureInPictureAspectRatioPolicy.Ratio classic = PictureInPictureAspectRatioPolicy.resolve(1440, 1080);
        assertEquals(4, classic.width);
        assertEquals(3, classic.height);
    }

    @Test
    public void supportsPortraitWithinAndroidBounds() {
        PictureInPictureAspectRatioPolicy.Ratio portrait = PictureInPictureAspectRatioPolicy.resolve(1080, 1920);
        assertEquals(9, portrait.width);
        assertEquals(16, portrait.height);
    }

    @Test
    public void fallsBackForUnknownOrUnsupportedRatios() {
        PictureInPictureAspectRatioPolicy.Ratio unknown = PictureInPictureAspectRatioPolicy.resolve(0, 0);
        assertEquals(16, unknown.width);
        assertEquals(9, unknown.height);

        PictureInPictureAspectRatioPolicy.Ratio tooWide = PictureInPictureAspectRatioPolicy.resolve(3000, 1000);
        assertEquals(16, tooWide.width);
        assertEquals(9, tooWide.height);
    }
}
