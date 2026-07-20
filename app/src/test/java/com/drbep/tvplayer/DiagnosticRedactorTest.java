package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiagnosticRedactorTest {
    @Test
    public void sanitizeUrlKeepsOnlySchemeHostAndPathForAbsoluteUrls() {
        String input = "https://cdn.example.com/path/manifest.mpd?access_token=secret&signature=abc#frag";

        assertEquals("https://cdn.example.com/path/manifest.mpd", DiagnosticRedactor.sanitizeUrl(input));
    }

    @Test
    public void sanitizeUrlStripsQueryFromRelativeBackendPaths() {
        String input = "/api/offline/u7d/movistar-ism/stream?device_id=abc&token=secret";

        assertEquals("/api/offline/u7d/movistar-ism/stream", DiagnosticRedactor.sanitizeUrl(input));
    }

    @Test
    public void redactSensitiveTextMasksBearerAndKnownSecretParameters() {
        String input = "Bearer abc.def token=secret key=123 authorization=super X-TCDN-token=signed normal=value";

        assertEquals(
                "Bearer <redacted> token=<redacted> key=<redacted> authorization=<redacted> X-TCDN-token=<redacted> normal=value",
                DiagnosticRedactor.redactSensitiveText(input)
        );
    }

    @Test
    public void sanitizeUrlHandlesBlankValues() {
        assertEquals("", DiagnosticRedactor.sanitizeUrl(null));
        assertEquals("", DiagnosticRedactor.sanitizeUrl("   "));
    }

    @Test(timeout = 250L)
    public void redactSensitiveTextHandlesPlutoUrlWithoutRegexBacktracking() {
        String input = "https://cfd-v4-service-channel-stitcher-use1-1.prd.pluto.tv/v2/stitch/hls/channel/6130d8dc943001000708548d/master.m3u8";

        assertEquals(input, DiagnosticRedactor.redactSensitiveText(input));
    }

    @Test(timeout = 250L)
    public void redactSensitiveTextBoundsVeryLongInputs() {
        StringBuilder input = new StringBuilder(250_000);
        for (int index = 0; index < 250_000; index++) {
            input.append('a');
        }
        input.append(" token=secret");

        String redacted = DiagnosticRedactor.redactSensitiveText(input.toString());

        assertTrue(redacted.length() < 4200);
        assertTrue(redacted.endsWith("...[truncated]"));
    }

    @Test
    public void redactSensitiveTextKeepsSeparatorsAndMixedCaseKeys() {
        String input = "prefix AUTHORIZATION=secret&normal=value bEaReR  abc.DEF tail";

        assertEquals(
                "prefix AUTHORIZATION=<redacted>&normal=value bEaReR  <redacted> tail",
                DiagnosticRedactor.redactSensitiveText(input)
        );
    }
}
