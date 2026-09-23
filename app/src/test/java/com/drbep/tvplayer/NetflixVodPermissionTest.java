package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class NetflixVodPermissionTest {
    @Test public void snapshotRequiresBothExplicitFlags() throws Exception {
        assertFalse(CatalogRepository.parseOfflinePermissions(new org.json.JSONObject("{}" )).allowsNetflixVod());
        assertFalse(CatalogRepository.parseOfflinePermissions(new org.json.JSONObject("{\"permissions\":{\"vod\":true}}" )).allowsNetflixVod());
        assertFalse(CatalogRepository.parseOfflinePermissions(new org.json.JSONObject("{\"permissions\":{\"vod\":false,\"netflix_vod\":true}}" )).allowsNetflixVod());
        assertTrue(CatalogRepository.parseOfflinePermissions(new org.json.JSONObject("{\"permissions\":{\"vod\":true,\"netflix_vod\":true}}" )).allowsNetflixVod());
    }
    @Test public void explicitPermissionAndGlobalVodAreBothRequired() {
        OfflinePermissions p = new OfflinePermissions();
        p.vodEnabled = true;
        p.appleTVVodEnabled = p.hboVodEnabled = true;
        assertFalse(VodOriginPolicy.allowed(p, "NETFLIX"));
        p.netflixVodEnabled = true;
        assertTrue(VodOriginPolicy.allowed(p, "NETFLIX"));
        p.vodEnabled = false;
        assertFalse(VodOriginPolicy.allowed(p, "NETFLIX"));
    }
    @Test public void netflixOnlyStillOpensLibraryAndNeverGrantsApple() {
        OfflinePermissions p = new OfflinePermissions();
        p.vodEnabled = true;
        p.netflixVodEnabled = true;
        assertTrue(p.hasVodCatalogAccess());
        assertFalse(p.allowsAppleTVVod());
        p.netflixVodEnabled = false;
        assertFalse(VodOriginPolicy.allowed(p, "NETFLIX"));
    }
}
