package com.drbep.tvplayer;

import org.junit.Test;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import static org.junit.Assert.*;

public class LicenseNetworkTransportTest {
    @Test public void retainsLicenseUrlBodyAndPerItemHeadersWithoutSending() {
        RequestBody body = RequestBody.create(MediaType.get("application/octet-stream"), new byte[]{1,2,3});
        Request request = new Request.Builder()
                .url("https://fire.tvbep.com/license/test")
                .header("Authorization", "test-only")
                .post(body).build();
        Call call = NetworkClients.licenseCallFactory().newCall(request);
        assertEquals(request.url(), call.request().url());
        assertEquals("POST", call.request().method());
        assertSame(body, call.request().body());
        assertEquals("test-only", call.request().header("Authorization"));
        assertFalse(call.isExecuted());
    }

    @Test public void unrelatedLicenseHostReceivesNoInheritedCredentials() {
        Call.Factory factory = NetworkClients.licenseCallFactory();
        factory.newCall(new Request.Builder().url("https://fire.tvbep.com/license/test")
                .header("Authorization", "test-only").build());
        Call independent = factory.newCall(new Request.Builder()
                .url("https://license.example.org/test").build());
        assertEquals("license.example.org", independent.request().url().host());
        assertNull(independent.request().header("Authorization"));
        assertNull(independent.request().header("Cookie"));
        assertFalse(independent.isExecuted());
    }
}
