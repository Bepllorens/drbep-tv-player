package com.drbep.tvplayer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChannelCollectionStoreTest {
    @Test
    public void defaultCollectionsAreAvailable() {
        ChannelCollectionStore store = new ChannelCollectionStore(null, "collections");

        List<ChannelCollectionStore.ChannelCollection> collections = store.getCollections();

        assertEquals(4, collections.size());
        assertEquals("Deportes", collections.get(0).label);
        assertEquals("Noticias", collections.get(1).label);
        assertEquals("Kids", collections.get(2).label);
        assertEquals("Mis canales", collections.get(3).label);
    }

    @Test
    public void membershipCanBeAddedAndRemoved() {
        ChannelCollectionStore store = new ChannelCollectionStore(null, "collections");

        store.setMembership("deportes", "ch-1", true);

        assertTrue(store.contains("deportes", "ch-1"));
        assertTrue(store.hasAnyMembership("ch-1"));
        assertEquals(1, store.getNonEmptyCollections().size());

        store.setMembership("deportes", "ch-1", false);

        assertFalse(store.contains("deportes", "ch-1"));
        assertFalse(store.hasAnyMembership("ch-1"));
        assertEquals(0, store.getNonEmptyCollections().size());
    }

    @Test
    public void collectionsCanBeCreatedRenamedAndDeleted() {
        ChannelCollectionStore store = new ChannelCollectionStore(null, "collections");

        ChannelCollectionStore.ChannelCollection created = store.createCollection("Cine");

        assertEquals("Cine", created.label);
        assertTrue(store.contains(created.key, "missing") == false);

        assertTrue(store.renameCollection(created.key, "Cine noche"));
        assertEquals("Cine noche", store.getCollections().get(4).label);

        assertTrue(store.deleteCollection(created.key));
        assertEquals(4, store.getCollections().size());
    }
}
