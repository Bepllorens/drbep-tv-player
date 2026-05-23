package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CatalogRepositoryTest {
    @Test
    public void enabledDashboardVodKeysKeepSpecificVodFiltersVisible() {
        CatalogRepository repository = new CatalogRepository("https://iptv.example.com");
        StartupFilterConfig startupConfig = new StartupFilterConfig();
        startupConfig.enabledFilterKeys.add("platform:1");
        startupConfig.enabledFilterKeys.add("vod:tivify:general");
        startupConfig.enabledFilterKeys.add("vod:tivify:adult");
        startupConfig.enabledFilterKeys.add("vod:runtime:movies");

        List<ChannelFilter> filters = repository.buildFiltersFromCatalog(
                Arrays.asList(
                        liveChannel(),
                        vodItem("Tivify Movie", false, "Tivify VOD", "vod:tivify:general"),
                        vodItem("Tivify Adult", true, "Tivify VOD", "vod:tivify:adult"),
                        vodItem("Runtime Movie", false, "Runtime Peliculas", "vod:runtime:movies")
                ),
                1L,
                startupConfig,
                null
        );

        assertTrue(hasFilter(filters, "favorites", 5));
        assertTrue(hasFilter(filters, "platform:1", 1));
        assertTrue(hasFilter(filters, "vod:tivify:general", 3));
        assertTrue(hasFilter(filters, "vod:tivify:adult", 4));
        assertTrue(hasFilter(filters, "vod:runtime:movies", 3));
        assertFalse(hasFilter(filters, "vod", 3));
        assertFalse(hasFilter(filters, "vod-adult", 4));
    }

    @Test
    public void noStartupFilterKeepsOnlySpecificVodFiltersWhenAvailable() {
        CatalogRepository repository = new CatalogRepository("https://iptv.example.com");

        List<ChannelFilter> filters = repository.buildFiltersFromCatalog(
                Arrays.asList(
                        liveChannel(),
                        vodItem("Tivify Movie", false, "Tivify VOD", "vod:tivify:general"),
                        vodItem("Runtime Movie", false, "Runtime Peliculas", "vod:runtime:movies")
                ),
                0L,
                new StartupFilterConfig(),
                null
        );

        assertTrue(hasFilter(filters, "vod:tivify:general", 3));
        assertTrue(hasFilter(filters, "vod:runtime:movies", 3));
        assertFalse(hasFilter(filters, "vod", 3));
    }

    @Test
    public void missingStartupFilterDoesNotLeaveOnlyEmptyFavorites() {
        CatalogRepository repository = new CatalogRepository("https://iptv.example.com");
        StartupFilterConfig startupConfig = new StartupFilterConfig();
        startupConfig.enabledFilterKeys.add("custom-group:deportes");

        List<ChannelFilter> filters = repository.buildFiltersFromCatalog(
                Arrays.asList(liveChannel()),
                0L,
                startupConfig,
                null
        );

        assertTrue(hasFilter(filters, "favorites", 5));
        assertTrue(hasFilter(filters, "platform:1", 1));
    }

    private static boolean hasFilter(List<ChannelFilter> filters, String key, int type) {
        for (ChannelFilter filter : filters) {
            if (filter != null && key.equals(filter.key) && filter.type == type) {
                return true;
            }
        }
        return false;
    }

    private static ChannelItem liveChannel() {
        return new ChannelItem(
                "1",
                "Live",
                "",
                "",
                "General",
                "https://iptv.example.com/live/1",
                "https://iptv.example.com/proxy/manifest/1",
                1,
                1,
                false,
                false,
                1,
                "Live Platform",
                new ArrayList<>(),
                "",
                "",
                "",
                false
        );
    }

    private static ChannelItem vodItem(String title, boolean adult, String platformName, String vodFilterKey) {
        return new ChannelItem(
                "vod-" + title.replace(" ", "-").toLowerCase(),
                title,
                "",
                "",
                "VOD",
                "https://vod.example.com/" + title.replace(" ", "-").toLowerCase() + ".m3u8",
                "",
                10,
                10,
                true,
                adult,
                0,
                platformName,
                new ArrayList<>(),
                "",
                "",
                vodFilterKey,
                true
        );
    }
}
