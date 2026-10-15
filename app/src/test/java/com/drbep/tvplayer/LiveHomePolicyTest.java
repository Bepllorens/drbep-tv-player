package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class LiveHomePolicyTest {
    @Test public void platformLookupDoesNotChangeGroupMembershipOrAcceptPartialIds() {
        ChannelItem item = channel();
        assertTrue(LiveHomePolicy.matches(item, "live:42"));
        assertFalse(LiveHomePolicy.matches(item, "live:4"));
        assertFalse(LiveHomePolicy.matches(item, "live:420"));
        assertTrue(LiveHomePolicy.matches(item, "group:deportes"));
        assertFalse(LiveHomePolicy.matches(item, "group:otro"));
        assertFalse(LiveHomePolicy.matches(item, null));
    }

    private ChannelItem channel() {
        return new ChannelItem("one", "Uno", "", "", "Deportes", "", "", 0, 0, false, false, 42, "Operador", Arrays.asList("Deportes"), "", "", "", false);
    }
    private OfflinePermissions permissions() {
        OfflinePermissions p = new OfflinePermissions(); p.liveAccessMetadataKnown = true; return p;
    }
    @Test public void actionLaunchersCannotStartFromPreview() {
        ChannelItem launcher = new ChannelItem(DaznEventPolicy.COMPETITIONS_CHANNEL_ID, "DAZN", "", "", "", DaznEventPolicy.COMPETITIONS_PLAY_URL, "", 0, 0, false, false, 42, "DAZN", Collections.emptyList(), "", "", "", false);
        assertFalse(LiveHomePolicy.isLiveChannel(launcher));
        assertTrue(LiveHomePolicy.isLiveChannel(channel()));
    }
    @Test public void groupGrantDoesNotExposeWholePlatform() {
        OfflinePermissions p = permissions(); p.allowedLiveGroups.add("deportes");
        List<StartupHomeHubUiModel.Provider> categories = LiveHomePolicy.categories(Arrays.asList(channel()), p);
        assertEquals(2, categories.size()); assertEquals("group:deportes", categories.get(1).key);
        assertTrue(LiveHomePolicy.matches(channel(), categories.get(1).key));
        assertFalse(LiveHomePolicy.platformAllowed(p, 42));
    }
    @Test public void individualGrantRemainsInAllWithoutInventingPlatformGrant() {
        assertEquals(1, LiveHomePolicy.categories(Arrays.asList(channel()), permissions()).size());
        assertTrue(LiveHomePolicy.matches(channel(), "ALL"));
    }
    @Test public void platformGrantDoesNotInventGroupGrant() {
        OfflinePermissions p = permissions(); p.allowedPlatformIds.add(42);
        List<StartupHomeHubUiModel.Provider> categories = LiveHomePolicy.categories(Arrays.asList(channel()), p);
        assertEquals(2, categories.size()); assertEquals("live:42", categories.get(1).key);
    }
    @Test public void disabledLiveAndEmptyCatalogHaveNoCategories() {
        OfflinePermissions p = permissions(); p.admin = true; p.liveEnabled = false;
        assertTrue(LiveHomePolicy.categories(Arrays.asList(channel()), p).isEmpty());
        p.liveEnabled = true; assertTrue(LiveHomePolicy.categories(Collections.emptyList(), p).isEmpty());
    }
    @Test public void adminMustHaveExplicitAssignments() {
        OfflinePermissions p = permissions(); p.admin = true;
        assertFalse(LiveHomePolicy.platformAllowed(p, 42));
        assertFalse(LiveHomePolicy.groupAllowed(p, "Deportes"));
        assertFalse(LiveHomePolicy.visibleInHome(channel(), p));
        p.allowedLiveGroups.add("deportes");
        assertTrue(LiveHomePolicy.visibleInHome(channel(), p));
        assertFalse(LiveHomePolicy.platformAllowed(p, 42));
        assertEquals(2, LiveHomePolicy.categories(Arrays.asList(channel()), p).size());
        p.allowedPlatformIds.add(42);
        assertEquals(3, LiveHomePolicy.categories(Arrays.asList(channel()), p).size());
        p.liveAccessMetadataKnown = false;
        assertFalse(LiveHomePolicy.visibleInHome(channel(), p));
    }
}
