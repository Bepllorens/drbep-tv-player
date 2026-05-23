package com.drbep.tvplayer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChannelOverlayCoordinatorTest {
    @Test
    public void localCollectionsAndHiddenProfilesBecomeFilters() {
        List<ChannelItem> visible = new ArrayList<>();
        List<ChannelItem> all = new ArrayList<>();
        List<ChannelFilter> filters = new ArrayList<>();
        ChannelCollectionStore collections = new ChannelCollectionStore(null, "collections");
        ChannelProfileStore profiles = new ChannelProfileStore(null, "profiles");
        collections.setMembership("deportes", "ch-1", true);
        profiles.setHidden("ch-2", true);

        ChannelOverlayCoordinator coordinator = new ChannelOverlayCoordinator(
                visible,
                all,
                filters,
                new HashSet<>(),
                null,
                collections,
                profiles
        );

        coordinator.applyLoadedChannels(new CatalogLoadResult(sampleChannels(), baseFilters(), "all"), "ch-1");

        assertEquals(1, visible.size());
        assertEquals("ch-1", visible.get(0).id);
        assertTrue(hasFilter(filters, "collection:deportes"));
        assertTrue(hasFilter(filters, "hidden"));

        coordinator.setSelectedFilterKey("collection:deportes");
        coordinator.refreshVisibleChannels("ch-1", "ch-1");

        assertEquals(1, visible.size());
        assertEquals("ch-1", visible.get(0).id);

        coordinator.setSelectedFilterKey("hidden");
        coordinator.refreshVisibleChannels("ch-1", "ch-2");

        assertEquals(1, visible.size());
        assertEquals("ch-2", visible.get(0).id);
    }

    @Test
    public void searchMatchesChannelAlias() {
        List<ChannelItem> visible = new ArrayList<>();
        List<ChannelItem> all = new ArrayList<>();
        List<ChannelFilter> filters = new ArrayList<>();
        ChannelProfileStore profiles = new ChannelProfileStore(null, "profiles");
        profiles.setAlias("ch-1", "Canal sofa");

        ChannelOverlayCoordinator coordinator = new ChannelOverlayCoordinator(
                visible,
                all,
                filters,
                new HashSet<>(),
                null,
                new ChannelCollectionStore(null, "collections"),
                profiles
        );
        coordinator.applyLoadedChannels(new CatalogLoadResult(sampleChannels(), baseFilters(), "all"), "ch-1");

        coordinator.setSearchQuery("sofa");
        coordinator.refreshVisibleChannels("ch-1", "ch-1");

        assertEquals(1, visible.size());
        assertEquals("ch-1", visible.get(0).id);
    }

    @Test
    public void restoredEmptyFavoritesFilterFallsBackToDefaultFilter() {
        List<ChannelItem> visible = new ArrayList<>();
        List<ChannelItem> all = new ArrayList<>();
        List<ChannelFilter> filters = new ArrayList<>();

        ChannelOverlayCoordinator coordinator = new ChannelOverlayCoordinator(
                visible,
                all,
                filters,
                new HashSet<>(),
                null,
                new ChannelCollectionStore(null, "collections"),
                new ChannelProfileStore(null, "profiles")
        );
        coordinator.setSelectedFilterKey("favorites");

        coordinator.applyLoadedChannels(new CatalogLoadResult(sampleChannels(), baseFilters(), "all"), "ch-1");

        assertEquals("all", coordinator.getSelectedFilterKey());
        assertEquals(2, visible.size());
        assertEquals("ch-1", visible.get(0).id);
    }

    @Test
    public void restoredEmptyFavoritesFilterFallsBackToFirstPlatformWhenAllIsDisabled() {
        List<ChannelItem> visible = new ArrayList<>();
        List<ChannelItem> all = new ArrayList<>();
        List<ChannelFilter> filters = new ArrayList<>();
        List<ChannelFilter> restrictedFilters = new ArrayList<>();
        restrictedFilters.add(new ChannelFilter("favorites", "Favoritos", 5, 0, ""));
        restrictedFilters.add(new ChannelFilter("platform:1", "Plataforma: TV", 1, 1, ""));

        ChannelOverlayCoordinator coordinator = new ChannelOverlayCoordinator(
                visible,
                all,
                filters,
                new HashSet<>(),
                null,
                new ChannelCollectionStore(null, "collections"),
                new ChannelProfileStore(null, "profiles")
        );
        coordinator.setSelectedFilterKey("favorites");

        coordinator.applyLoadedChannels(new CatalogLoadResult(sampleChannels(), restrictedFilters, "favorites"), "ch-1");

        assertEquals("platform:1", coordinator.getSelectedFilterKey());
        assertEquals(2, visible.size());
    }

    private static boolean hasFilter(List<ChannelFilter> filters, String key) {
        for (ChannelFilter filter : filters) {
            if (filter != null && key.equals(filter.key)) {
                return true;
            }
        }
        return false;
    }

    private static List<ChannelFilter> baseFilters() {
        List<ChannelFilter> filters = new ArrayList<>();
        filters.add(new ChannelFilter("all", "Todos", 0, 0, ""));
        filters.add(new ChannelFilter("favorites", "Favoritos", 5, 0, ""));
        return filters;
    }

    private static List<ChannelItem> sampleChannels() {
        List<ChannelItem> channels = new ArrayList<>();
        channels.add(channel("ch-1", "Canal Uno"));
        channels.add(channel("ch-2", "Canal Dos"));
        return channels;
    }

    private static ChannelItem channel(String id, String name) {
        return new ChannelItem(
                id,
                name,
                "",
                "",
                "General",
                "http://example.test/" + id,
                "",
                1,
                1,
                false,
                false,
                1,
                "TV",
                new ArrayList<>(),
                "",
                "",
                "",
                false
        );
    }
}
