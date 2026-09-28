package com.drbep.tvplayer;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class PrimeLicenseContextTest {
    private Map<String, String> playback() {
        Map<String, String> result = new HashMap<>();
        result.put("X-DRBEP-Prime-Profile", "android-uhd");
        result.put("X-DRBEP-Device-Id", "test-device");
        result.put("Authorization", "Bearer must-not-be-copied");
        result.put("X-DRBEP-Access-Token", "must-not-be-copied");
        return result;
    }

    @Test public void matchesManifestContextWithoutCopyingTokens() {
        Map<String, String> result = PrimeLicenseContext.headers("https://backend.example",
                "https://backend.example:443/api/vod/prime/license/title?access_token=existing", playback());
        assertEquals(2, result.size());
        assertEquals("android-uhd", result.get("X-DRBEP-Prime-Profile"));
        assertEquals("test-device", result.get("X-DRBEP-Device-Id"));
    }

    @Test public void rejectsOtherOriginsAndOtherProviders() {
        for (String url : new String[]{"https://provider.example/api/vod/prime/license/title",
                "https://backend.example.attacker.invalid/api/vod/prime/license/title",
                "http://backend.example/api/vod/prime/license/title",
                "https://backend.example:444/api/vod/prime/license/title",
                "https://backend.example/api/vod/private/hbomax/license/title",
                "https://user@backend.example/api/vod/prime/license/title", "invalid url"}) {
            assertTrue(PrimeLicenseContext.headers("https://backend.example", url, playback()).isEmpty());
        }
    }
}
