package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class VodOriginPolicyTest {
    @Test public void appleRequiresOwnPermissionAndBaseVod() {
        OfflinePermissions p = new OfflinePermissions(); p.vodEnabled = true;
        p.hboVodEnabled = p.disneyplusVodEnabled = true;
        assertFalse(VodOriginPolicy.allowed(p, "APPLETV"));
        p.appleTVVodEnabled = true;
        assertTrue(VodOriginPolicy.allowed(p, "APPLETV"));
        p.vodEnabled = false;
        assertFalse(VodOriginPolicy.allowed(p, "APPLETV"));
    }
    @Test public void globalDenialWinsOverProviderFlags() {
        OfflinePermissions p = new OfflinePermissions();
        p.skyVodEnabled = p.hboVodEnabled = true;
        for (MainActivity.VodVisualPlatformFilter origin : MainActivity.VodVisualPlatformFilter.values()) assertFalse(VodOriginPolicy.allowed(p, origin.name()));
        assertFalse(VodOriginPolicy.allowed(null, "ALL"));
    }
    @Test public void privateOriginsRequireTheirOwnPermission() {
        OfflinePermissions p = new OfflinePermissions(); p.vodEnabled = true;
        assertFalse(VodOriginPolicy.allowed(p, "HBO")); assertFalse(VodOriginPolicy.allowed(p, "SKYSHOWTIME"));
        p.skyVodEnabled = true;
        assertTrue(VodOriginPolicy.allowed(p, "SKYSHOWTIME")); assertFalse(VodOriginPolicy.allowed(p, "HBO"));
        p.hboVodEnabled = true; assertTrue(VodOriginPolicy.allowed(p, "HBO"));
    }
    @Test public void standardOriginsRespectExplicitDenials() {
        OfflinePermissions p = new OfflinePermissions(); p.vodEnabled = true;
        p.primeVodEnabled = p.daznVodEnabled = p.plexVodEnabled = false;
        assertFalse(VodOriginPolicy.allowed(p, "PRIME")); assertFalse(VodOriginPolicy.allowed(p, "DAZN")); assertFalse(VodOriginPolicy.allowed(p, "PLEX"));
        assertFalse(VodOriginPolicy.allowed(p, "OTHER"));
    }
    @Test public void disneyplusRequiresItsOwnPermission() {
        OfflinePermissions p = new OfflinePermissions(); p.vodEnabled = true;
        assertFalse(VodOriginPolicy.allowed(p, "DISNEYPLUS"));
        p.disneyplusVodEnabled = true;
        assertTrue(VodOriginPolicy.allowed(p, "DISNEYPLUS"));
    }
}
