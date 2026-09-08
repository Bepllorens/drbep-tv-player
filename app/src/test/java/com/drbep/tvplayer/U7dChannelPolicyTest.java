package com.drbep.tvplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

public class U7dChannelPolicyTest {
    @Test
    public void supportsMovistarDashAndMovistarIsm() {
        ChannelItem dash = channel(
                "1105031",
                "LA 1",
                1,
                "Movistar",
                "https://tvela1-dash-movistarplus.example/manifest.mpd"
        );
        ChannelItem ism = channel(
                "1105475",
                "LA 1",
                13,
                "Movistar ISM",
                "http://tvela1-pry-movistarplus.example/index.isml/Manifest"
        );

        assertTrue(U7dChannelPolicy.supports(dash));
        assertTrue(U7dChannelPolicy.supports(ism));
        assertFalse(U7dChannelPolicy.isMovistarIsmChannel(dash));
        assertTrue(U7dChannelPolicy.isMovistarIsmChannel(ism));
        assertTrue(U7dChannelPolicy.supportsRecordingStartOver(dash));
        assertTrue(U7dChannelPolicy.supportsRecordingStartOver(ism));
    }

    @Test
    public void excludesVodAndUnrelatedLivePlatforms() {
        ChannelItem unrelated = channel("7", "Canal", 7, "Pluto TV", "https://example.test/live.m3u8");
        ChannelItem vod = new ChannelItem(
                "vod-1",
                "Pelicula",
                "",
                "",
                "Movistar",
                "https://example.test/movie.mpd",
                "",
                1,
                1,
                true,
                false,
                1,
                "Movistar",
                Collections.emptyList(),
                "",
                "",
                "",
                true
        );

        assertFalse(U7dChannelPolicy.supports(unrelated));
        assertFalse(U7dChannelPolicy.supports(vod));
        assertFalse(U7dChannelPolicy.supportsRecordingStartOver(unrelated));
        assertFalse(U7dChannelPolicy.supportsRecordingStartOver(vod));
    }

    @Test
    public void recordingStartOverIncludesTivify() {
        ChannelItem tivifyDash = channel("1103933", "LA 1", 2, "Tivify", "https://example.test/live.mpd");
        ChannelItem tivifyHls = channel("1103934", "LA 2", 2, "Tivify", "https://example.test/live.m3u8");

        assertTrue(U7dChannelPolicy.supportsRecordingStartOver(tivifyDash));
        assertTrue(U7dChannelPolicy.supportsRecordingStartOver(tivifyHls));
    }

    private static ChannelItem channel(String id, String name, int platformId, String platform, String url) {
        return new ChannelItem(
                id,
                name,
                "",
                "",
                platform,
                url,
                "",
                1,
                1,
                false,
                false,
                platformId,
                platform,
                Collections.emptyList(),
                "",
                "",
                "",
                true
        );
    }
}
