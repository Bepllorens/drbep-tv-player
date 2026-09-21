package com.drbep.tvplayer;

import org.junit.Test;
import okhttp3.Request;
import static org.junit.Assert.*;

public class NetworkClientsTest {
    @Test public void disneyCdnNeverReceivesBackendCredentials() {
        Request input = new Request.Builder().url("https://video.media.dssott.com/movie/segment.mp4")
                .header("Authorization", "Bearer test-session")
                .header("X-DRBEP-Access-Token", "test-session")
                .header("X-DRBEP-Device-Id", "test-device")
                .header("X-DRBEP-Prime-Profile", "android-uhd")
                .header("Cookie", "session=test-session")
                .header("Range", "bytes=0-512").build();
        Request output = NetworkClients.disneyMediaRequest(input);
        assertNull(output.header("Authorization"));
        assertNull(output.header("X-DRBEP-Access-Token"));
        assertNull(output.header("X-DRBEP-Device-Id"));
        assertNull(output.header("X-DRBEP-Prime-Profile"));
        assertNull(output.header("Cookie"));
        assertEquals("bytes=0-512", output.header("Range"));
        assertEquals(input.url(), output.url());
    }

    @Test public void backendLicenseKeepsAuthentication() {
        Request input = new Request.Builder().url("https://example.test/api/vod/private/disneyplus/license/id")
                .header("Authorization", "Bearer test-session").build();
        assertSame(input, NetworkClients.disneyMediaRequest(input));
    }
}
