package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class U7dEngineSafetyTest {
    @Test public void protectedAndReplayNeverUseUnprotectedEngine() {
        assertFalse(VlcFallbackPolicy.allowsUnprotectedEngine("widevine", ""));
        assertFalse(VlcFallbackPolicy.allowsUnprotectedEngine("clearkey", ""));
        assertFalse(VlcFallbackPolicy.allowsUnprotectedEngine("", "u7d_proxy"));
        assertTrue(VlcFallbackPolicy.allowsUnprotectedEngine("", "plex-avi-direct"));
    }
}
