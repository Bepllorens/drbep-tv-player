package com.drbep.tvplayer;
import org.junit.Test;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;
public class PlaylistPlaybackConfigTest {
    @Test public void separatesStreamAndLicenseHeadersAndPipeOverrides() throws Exception {
        JSONObject d=new JSONObject().put("extvlcopt:http-user-agent","First")
            .put("kodiprop:inputstream.adaptive.stream_headers","Referer=https%3A%2F%2Fexample.test%2F")
            .put("kodiprop:inputstream.adaptive.license_type","com.widevine.alpha")
            .put("kodiprop:inputstream.adaptive.license_key","https://license.test/wv|Authorization=Bearer%20fixture|R{SSM}|B")
            .put("kodiprop:inputstream.adaptive.manifest_type","mpd");
        PlaylistPlaybackConfig c=PlaylistPlaybackConfig.parse("https://video.test/play|User-Agent=Last",d);
        assertEquals("Last",c.streamHeaders.get("User-Agent"));
        assertFalse(c.streamHeaders.containsKey("Authorization"));
        assertEquals("Bearer fixture",c.licenseHeaders.get("Authorization"));
        assertFalse(c.licenseHeaders.containsKey("Referer"));
        assertEquals("application/dash+xml",c.mime);
        assertEquals("https://video.test/play",c.url);
    }
    @Test public void inlineClearKeyProducesValidLocalResponse() throws Exception {
        PlaylistPlaybackConfig c=PlaylistPlaybackConfig.parse("https://video.test/play.mpd",new JSONObject()
            .put("kodiprop:inputstream.adaptive.license_type","org.w3.clearkey")
            .put("kodiprop:inputstream.adaptive.license_key","00112233445566778899aabbccddeeff:ffeeddccbbaa99887766554433221100"));
        JSONObject jwk=new JSONObject(new String(Base64.getDecoder().decode(c.license.substring(c.license.indexOf(',')+1)),StandardCharsets.UTF_8));
        assertEquals(16,Base64.getUrlDecoder().decode(jwk.getJSONArray("keys").getJSONObject(0).getString("k")).length);
        assertEquals("clearkey",c.scheme);
    }
    @Test public void kodiJsonMappingPreservesEveryKey() throws Exception {
        JSONObject mapping = new JSONObject()
            .put("00112233445566778899aabbccddeeff", "ffeeddccbbaa99887766554433221100")
            .put("ffeeddccbbaa99887766554433221100", "00112233445566778899aabbccddeeff");
        PlaylistPlaybackConfig c = PlaylistPlaybackConfig.parse("https://video.test/play.mpd", new JSONObject()
            .put("kodiprop:inputstream.adaptive.license_type", "clearkey")
            .put("kodiprop:inputstream.adaptive.license_key", mapping.toString()));
        JSONObject response = new JSONObject(new String(Base64.getDecoder().decode(c.license.split(",",2)[1]), StandardCharsets.UTF_8));
        JSONArray keys = response.getJSONArray("keys");
        assertEquals(2, keys.length());
        for (int i=0; i<keys.length(); i++) {
            JSONObject key=keys.getJSONObject(i);
            byte[] id=Base64.getUrlDecoder().decode(key.getString("kid"));
            byte[] value=Base64.getUrlDecoder().decode(key.getString("k"));
            assertEquals(16,id.length);assertEquals(16,value.length);
            for(int j=0;j<16;j++)assertEquals(255,(id[j]&255)+(value[j]&255));
        }
        // Already normalized JWK remains accepted.
        PlaylistPlaybackConfig normalized=PlaylistPlaybackConfig.parse("https://video.test/play.mpd",new JSONObject()
            .put("kodiprop:inputstream.adaptive.license_type","clearkey")
            .put("kodiprop:inputstream.adaptive.license_key",response.toString()));
        assertEquals(c.license,normalized.license);
    }
    @Test public void invalidJsonMappingsAreRejected() throws Exception {
        for(String mapping:new String[]{"{}", "{\"invalid\":\"invalid\"}"}) {
            try {
                PlaylistPlaybackConfig.parse("https://video.test/play.mpd",new JSONObject()
                    .put("kodiprop:inputstream.adaptive.license_type","clearkey")
                    .put("kodiprop:inputstream.adaptive.license_key",mapping));
                fail("Invalid mapping accepted");
            } catch(IllegalArgumentException expected) {}
        }
    }
    @Test public void requestCopiesPlaylistHeadersAcrossRetries() {
        Map<String,String> h=new HashMap<>();h.put("Referer","https://example.test");
        PlayerController.PlaybackRequest r=new PlayerController.PlaybackRequest("m3u:x","x","","https://video.test","","direct","","",true)
            .withPlaylistHeaders(h,Collections.emptyMap(),"application/dash+xml");
        h.clear(); assertEquals(1,r.playlistStreamHeaders.size());
    }
    @Test public void malformedInputsFailClosed() throws Exception {
        for(String url:new String[]{"file:///etc/passwd","https://video.test|User-Agent=x%0d%0aBad%3Ayes","https://video.test|Host=other"}) {
            try { PlaylistPlaybackConfig.parse(url,new JSONObject()); fail(url); } catch(IllegalArgumentException expected) {}
        }
        try { PlaylistPlaybackConfig.parse("https://video.test",new JSONObject().put("kodiprop:inputstream.adaptive.license_type","widevine").put("kodiprop:inputstream.adaptive.license_key","https://license.test||custom-body|B")); fail(); } catch(IllegalArgumentException expected) {}
    }
}
