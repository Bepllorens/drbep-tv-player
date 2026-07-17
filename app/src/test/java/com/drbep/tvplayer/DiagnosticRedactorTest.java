package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

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
}
