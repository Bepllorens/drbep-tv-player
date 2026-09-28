package com.drbep.tvplayer.netflix;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetflixMediaUrlsTest {
    @Test public void resolvesNestedCdnUrlWithoutChangingItsQuery() throws Exception {
        String url = "https://video.nflxvideo.net/movie?token=test&range=1";
        JSONObject locations = new JSONObject().put("cdn", new JSONArray().put(new JSONObject().put("url", url)));
        assertEquals(url, NetflixMediaUrls.findUrl(locations, 0));
    }
    @Test public void rejectsOtherOriginsCredentialsAndCleartext() {
        for (String url : new String[] {"http://video.nflxvideo.net/file", "https://nflxvideo.net.attacker.example/file",
                "https://user@video.nflxvideo.net/file", "https://example.org/file", "file:///tmp/movie"})
            assertNull(NetflixMediaUrls.findUrl(url, 0));
    }
    @Test public void boundsNestedManifestTraversal() throws Exception {
        Object value = "https://video.nflxvideo.net/movie";
        for (int i = 0; i < 7; i++) value = new JSONArray().put(value);
        assertNull(NetflixMediaUrls.findUrl(value, 0));
    }
}
