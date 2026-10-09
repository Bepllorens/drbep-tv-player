package com.drbep.tvplayer;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LiveHomeFeedTest {
    private ChannelItem channel(String id, int platform) {
        return new ChannelItem(id, id, "", "", "TDT", "https://example.test/live", "", 0, 0,
                false, false, platform, "Plataforma", Collections.singletonList("TDT"), "", "", "", false);
    }
    private OfflinePermissions permissions() {
        OfflinePermissions p = new OfflinePermissions();
        p.liveEnabled = true; p.liveAccessMetadataKnown = true; p.allowedPlatformIds.add(42);
        return p;
    }
    @Test public void missingGroupNeverFallsBackToAllChannels() {
        List<ChannelItem> input = Arrays.asList(channel("a",42));
        assertTrue(LiveHomeFeed.select(input, "", permissions(), 8).isEmpty());
        assertTrue(LiveHomeFeed.select(input, "ALL", permissions(), 8).isEmpty());
        assertTrue(LiveHomeFeed.select(input, null, permissions(), 8).isEmpty());
    }
    @Test public void preservesFavoriteThenRecentOrderWithoutDuplicates() {
        ChannelItem favorite = channel("favorite",42), recent = channel("recent",42);
        List<ChannelItem> rows = LiveHomeFeed.select(Arrays.asList(favorite, favorite, recent), "live:42", permissions(), 8);
        assertEquals(2, rows.size()); assertSame(favorite, rows.get(0)); assertSame(recent, rows.get(1));
    }
    @Test public void excludesOtherPlatformsAndCapsWork() {
        List<ChannelItem> rows = LiveHomeFeed.select(Arrays.asList(channel("other",7), channel("one",42), channel("two",42)), "live:42", permissions(), 1);
        assertEquals(1, rows.size()); assertEquals("one", rows.get(0).id);
    }
    @Test public void revokedLiveAccessEmptiesHome() {
        OfflinePermissions p = permissions(); p.liveEnabled = false;
        assertTrue(LiveHomeFeed.select(Arrays.asList(channel("a",42)), "live:42", p, 8).isEmpty());
    }
    @Test public void revokedCategoryCannotExposeCachedChannels() {
        OfflinePermissions p = permissions(); p.allowedPlatformIds.clear();
        assertTrue(LiveHomeFeed.select(Arrays.asList(channel("a",42)), "live:42", p, 8).isEmpty());
    }
}
