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
    public void channelArtworkKeepsPlatformAndGroupLogosSeparate() {
        ChannelItem item = liveChannel();
        item.platformLogoUrl = "https://example.test/platform.png";
        item.customGroupLogos.put("deportes", "https://example.test/sports.png");

        assertEquals("https://example.test/platform.png", item.platformLogoUrl);
        assertEquals("https://example.test/sports.png", item.customGroupLogo("Deportes"));
        assertEquals("", item.customGroupLogo("Noticias"));
    }

    @Test
    public void nowPlayingModelUsesLogoAndKeepsInitialsAsFallback() {
        ChannelOverlayUi.NowPlayingModel withLogo = new ChannelOverlayUi.NowPlayingModel(
                "Canal", "Programa", "Ruta", "1080p", true, "Recientes", "Movistar ISM", "https://example.test/movistar.png"
        );
        ChannelOverlayUi.NowPlayingModel withoutLogo = new ChannelOverlayUi.NowPlayingModel(
                "Canal", "Programa", "Ruta", "1080p", true, "Recientes", "Grupo Deportes"
        );

        assertEquals("https://example.test/movistar.png", withLogo.contextLogoUrl);
        assertEquals("", withoutLogo.contextLogoUrl);
        assertFalse(withoutLogo.contextInitials.isEmpty());
    }

    @Test
    public void standaloneVodApiUsesPublicOfflineBaseUrl() {
        CatalogRepository repository = new CatalogRepository("https://fire.tvbep.com", null, true);

        assertEquals("https://fire.tvbep.com", repository.vodApiBaseUrl());
    }

    @Test
    public void enabledDashboardVodKeysKeepSpecificVodFiltersVisible() {
        CatalogRepository repository = new CatalogRepository("https://iptv.example.com");
        StartupFilterConfig startupConfig = new StartupFilterConfig();
        startupConfig.enabledFilterKeys.add("platform:1");
        startupConfig.enabledFilterKeys.add("vod:tivify:general");
        startupConfig.enabledFilterKeys.add("vod:tivify:adult");
        startupConfig.enabledFilterKeys.add("vod:runtime:movies");
        startupConfig.enabledFilterKeys.add("vod:plex:movies");
        startupConfig.enabledFilterKeys.add("vod:plex:series");
        startupConfig.enabledFilterKeys.add("vod:dazn:live");
        startupConfig.enabledFilterKeys.add("vod:dazn:replay");
        startupConfig.enabledFilterKeys.add("vod:dazn:scheduled");
        startupConfig.enabledFilterKeys.add("vod:dazn:ondemand");

        List<ChannelFilter> filters = repository.buildFiltersFromCatalog(
                Arrays.asList(
                        liveChannel(),
                        vodItem("Tivify Movie", false, "Tivify VOD", "vod:tivify:general"),
                        vodItem("Tivify Adult", true, "Tivify VOD", "vod:tivify:adult"),
                        vodItem("Runtime Movie", false, "Runtime Peliculas", "vod:runtime:movies"),
                        vodItem("Plex Movie", false, "Plex", "vod:plex:movies"),
                        vodItem("Plex Episode", false, "Plex", "vod:plex:series"),
                        vodItem("DAZN Event", false, "DAZN", "vod:dazn:live"),
                        vodItem("DAZN Replay", false, "DAZN", "vod:dazn:replay"),
                        vodItem("DAZN Scheduled", false, "DAZN", "vod:dazn:scheduled"),
                        vodItem("DAZN Documentary", false, "DAZN", "vod:dazn:ondemand")
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
        assertTrue(hasFilter(filters, "vod:plex:movies", 3));
        assertTrue(hasFilter(filters, "vod:plex:series", 3));
        assertTrue(hasFilter(filters, "vod:dazn:live", 3));
        assertTrue(hasFilter(filters, "vod:dazn:replay", 3));
        assertTrue(hasFilter(filters, "vod:dazn:scheduled", 3));
        assertTrue(hasFilter(filters, "vod:dazn:ondemand", 3));
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

        assertTrue(hasFilter(filters, "all", 0));
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

    @Test
    public void offlinePermissionsProtectAdultVodAndConfiguredGroups() {
        OfflinePermissions permissions = new OfflinePermissions();
        permissions.protectAdultVod = true;
        permissions.protectedGroupNames.add("adultos");
        permissions.protectedFilterKeys.add("custom-group:adultos");
        permissions.protectedChannelIds.add("xxx-1");

        ChannelItem groupProtected = new ChannelItem(
                "tv-1",
                "Canal Adultos",
                "",
                "",
                "Adultos",
                "https://iptv.example.com/live/adultos",
                "",
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
        ChannelItem vodProtected = vodItem("Adult Feature", true, "Tivify VOD", "vod:tivify:adult");
        ChannelItem channelProtected = new ChannelItem(
                "xxx-1",
                "Canal X",
                "",
                "",
                "General",
                "https://iptv.example.com/live/xxx-1",
                "",
                2,
                2,
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

        assertTrue(permissions.isProtectedItem(groupProtected));
        assertTrue(permissions.isProtectedItem(vodProtected));
        assertTrue(permissions.isProtectedItem(channelProtected));
        assertTrue(permissions.isProtectedFilter(new ChannelFilter("custom-group:adultos", "Grupo: Adultos", 2, 0, "Adultos")));
        assertTrue(permissions.isProtectedFilter(new ChannelFilter("vod:tivify:adult", "Tivify Adulto", 4, 0, "")));
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
