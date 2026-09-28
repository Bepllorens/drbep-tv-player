package com.drbep.tvplayer.netflix;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetflixTracksTest {
    private JSONObject stream(int width,int height,int bitrate) throws Exception {
        return new JSONObject().put("resW",width).put("resH",height).put("bitrate",bitrate)
            .put("urls",new JSONObject().put("cdn","https://video.nflxvideo.net/stream"));
    }
    private JSONObject manifest() throws Exception {
        return new JSONObject().put("videoTracks",new JSONArray().put(new JSONObject().put("streams",new JSONArray()
            .put(stream(3840,2160,4000)).put(stream(1920,1080,7000)).put(stream(3840,2160,12000)))));
    }
    @Test public void highestResolutionUsesHighestBitrateRatherThanLowest() throws Exception {
        NetflixTracks choices=NetflixTracks.parse(manifest());
        assertEquals(3840,choices.video.get(0).width);assertEquals(12000,choices.video.get(0).bitrate);
        assertEquals(4000,choices.video.get(1).bitrate);assertEquals(1920,choices.video.get(2).width);
        assertEquals("12.00 Mb/s",NetflixTracks.bitrateLabel(12000));
    }
    @Test public void unapprovedStreamsAreNotSelectable() throws Exception {
        JSONObject m=manifest();m.getJSONArray("videoTracks").getJSONObject(0).getJSONArray("streams")
            .put(stream(7680,4320,20000).put("urls","https://example.org/invalid"));
        assertEquals(3,NetflixTracks.parse(m).video.size());
    }
    @Test public void selectsSpanishAudioAndRetainsOtherLanguages() throws Exception {
        JSONObject m=manifest().put("audioTracks",new JSONArray()
            .put(new JSONObject().put("language","en").put("streams",new JSONArray().put(stream(0,0,128))))
            .put(new JSONObject().put("language","es-ES").put("streams",new JSONArray().put(stream(0,0,96)).put(stream(0,0,192)))));
        NetflixTracks choices=NetflixTracks.parse(m);assertEquals(1,choices.defaultAudio());
        assertEquals(192,choices.audio.get(1).bitrate);assertEquals(2,choices.audio.size());
    }
    @Test public void onlySupportedWebVttSubtitlesBecomeChoices() throws Exception {
        JSONObject m=manifest().put("timedTextTracks",new JSONArray()
            .put(new JSONObject().put("language","es").put("ttDownloadables",new JSONObject()
                .put("webvtt-lssdh-ios8",new JSONObject().put("downloadUrls",new JSONObject().put("cdn","https://text.nflxvideo.net/es.vtt")))))
            .put(new JSONObject().put("language","en").put("ttDownloadables",new JSONObject()
                .put("dfxp",new JSONObject().put("url","https://text.nflxvideo.net/en.xml"))))
            .put(new JSONObject().put("isNoneTrack",true)));
        NetflixTracks choices=NetflixTracks.parse(new JSONObject().put("result",m));
        assertEquals(1,choices.subtitles.size());assertEquals("es",choices.subtitles.get(0).language);
    }
    @Test public void subtitleMenuPrioritizesFullSpanishAndLabelsForcedAndSdhCorrectly() throws Exception {
        JSONObject downloadable=new JSONObject().put("webvtt-lssdh-ios8",new JSONObject().put("url","https://text.nflxvideo.net/es.vtt"));
        JSONObject m=manifest().put("timedTextTracks",new JSONArray()
            .put(new JSONObject().put("language","es-ES").put("languageDescription","Desactivados")
                .put("isForcedNarrative",true).put("ttDownloadables",downloadable))
            .put(new JSONObject().put("language","en").put("ttDownloadables",downloadable))
            .put(new JSONObject().put("language","es-ES").put("languageDescription","Español (España)")
                .put("rawTrackType","ASSISTIVE").put("ttDownloadables",downloadable))
            .put(new JSONObject().put("language","es-ES").put("languageDescription","Español (España)")
                .put("ttDownloadables",downloadable)));
        NetflixTracks choices=NetflixTracks.parse(m);
        assertEquals("Español (España)",choices.subtitles.get(0).label);
        assertEquals("Español (España) · SDH",choices.subtitles.get(1).label);
        assertTrue(choices.subtitles.get(3).forced);
        assertFalse(choices.subtitles.get(3).label.contains("Desactivados"));
        assertTrue(choices.subtitles.get(3).label.endsWith(" · Forzados"));
    }

}
