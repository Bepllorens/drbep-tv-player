package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class SkyNativePlaybackTest {
    @Test public void replacesLegacyWvdRouteWithSameOriginNativeDrm() {
        SkyNativePlayback.Route route=SkyNativePlayback.resolve("https://fire.tvbep.com/api/vod/private/skyshowtime/play/asset?drm=clearkey&profile=uhd", "https://fire.tvbep.com");
        assertNotNull(route);
        assertEquals("https://fire.tvbep.com/api/vod/private/skyshowtime/play/asset?profile=hd",route.manifest);
        assertEquals("https://fire.tvbep.com/api/vod/private/skyshowtime/license/asset?profile=hd",route.license);
    }
    @Test public void refusesOtherOriginsAndProviders() {
        assertNull(SkyNativePlayback.resolve("https://foreign.example/api/vod/private/skyshowtime/play/asset", "https://fire.tvbep.com"));
        assertNull(SkyNativePlayback.resolve("https://fire.tvbep.com/api/vod/private/disneyplus/play/asset", "https://fire.tvbep.com"));
        assertNull(SkyNativePlayback.resolve("https://fire.tvbep.com/api/vod/private/skyshowtime/play/a%2Fb", "https://fire.tvbep.com"));
    }
    @Test public void retainsSameOriginAccessQueryWithoutRetainingOldDrmProfile() {
        SkyNativePlayback.Route route=SkyNativePlayback.resolve("https://fire.tvbep.com/api/vod/private/skyshowtime/play/asset?access_token=test&profile=uhd&drm=clearkey", "https://fire.tvbep.com");
        assertTrue(route.license.endsWith("?profile=hd&access_token=test"));
        assertFalse(route.manifest.contains("clearkey"));
    }
    @Test public void uhdStillUsesNativeLicenseAndRequiresEveryDeviceCapability() {
        assertTrue(SkyNativeCapabilities.allowsUhd("L1",true,true,true));
        assertFalse(SkyNativeCapabilities.allowsUhd("L3",true,true,true));
        assertFalse(SkyNativeCapabilities.allowsUhd("L1",false,true,true));
        assertFalse(SkyNativeCapabilities.allowsUhd("L1",true,false,true));
        assertFalse(SkyNativeCapabilities.allowsUhd("L1",true,true,false));
        SkyNativePlayback.Route route=SkyNativePlayback.resolve("https://fire.tvbep.com/api/vod/private/skyshowtime/play/asset?drm=clearkey", "https://fire.tvbep.com",true);
        assertTrue(route.manifest.endsWith("?profile=uhd"));
        assertTrue(route.license.contains("/skyshowtime/license/"));
        assertFalse(route.manifest.contains("clearkey"));
    }
}
