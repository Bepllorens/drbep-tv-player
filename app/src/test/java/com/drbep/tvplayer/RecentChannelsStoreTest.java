package com.drbep.tvplayer;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class RecentChannelsStoreTest {
    @Test public void mergePreservesReplayTitleAbsentFromCatalog() {
        RecentChannelsStore store = new RecentChannelsStore(null, "test");
        store.add("u7d:123:456", "Documental");
        store.mergeIds(Arrays.asList("u7d:123:456"), Collections.emptyMap());
        assertEquals("Documental", store.getItems().get(0).channelName);
        assertEquals(1, store.getItems().size());
    }

    @Test public void mergeUsesUpdatedCatalogName() {
        RecentChannelsStore store = new RecentChannelsStore(null, "test");
        store.add("123", "Antiguo");
        store.mergeIds(Arrays.asList("123"), Collections.singletonMap("123", "Nuevo"));
        assertEquals("Nuevo", store.getItems().get(0).channelName);
    }

    @Test public void unknownIdStillHasFallbackAndNoDuplicates() {
        RecentChannelsStore store = new RecentChannelsStore(null, "test");
        store.mergeIds(Arrays.asList("123", "123"), null);
        assertEquals("123", store.getItems().get(0).channelName);
        assertEquals(1, store.getItems().size());
    }
}
