package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class DisneyVodCatalogTest {
    private static final String REV = "0123456789abcdef0123456789abcdef";
    private static final String ID = "00000000-0000-4000-8000-000000000001";
    private static final String SHOW = "00000000-0000-4000-8000-000000000002";

    private JSONObject row(String id, String kind) throws Exception {
        JSONObject row = new JSONObject().put("id", id).put("kind", kind).put("title", "Title")
                .put("description", "Synopsis").put("duration_seconds", 1234).put("season", 2).put("episode", 3)
                .put("air_date", "2026-09-20").put("series_id", SHOW).put("series_title", "Show");
        if (!kind.equals("series")) {
            row.put("play_url", "/api/vod/private/disneyplus/play/" + id + "/master.m3u8")
                    .put("license_url", "/api/vod/private/disneyplus/license/" + id);
        }
        return row;
    }

    private JSONObject page(JSONArray items, String next) throws Exception {
        return new JSONObject().put("items", items).put("count", items.length()).put("next", next)
                .put("revision", REV).put("paged", true);
    }

    @Test public void catalogPinsRevisionAndLoadsParentsNotEpisodes() throws Exception {
        List<String> paths = new ArrayList<>();
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path -> {
            paths.add(path);
            if (path.contains("kind=movie")) return page(new JSONArray().put(row(ID, "movie")), "");
            return page(new JSONArray().put(row(SHOW, "series")), "");
        });
        List<ChannelItem> items = catalog.catalog();
        assertEquals(2, items.size());
        assertEquals(2, paths.size());
        assertTrue(paths.get(1).contains("revision=" + REV));
        assertEquals("https://example.test/api/vod/private/disneyplus/play/" + ID + "/master.m3u8", items.get(0).playUrl);
        assertEquals("widevine", items.get(0).drmScheme);
        assertEquals("Synopsis", items.get(0).vodDescription);
        assertEquals(1234L, items.get(0).vodDurationSeconds);
        assertEquals("2026-09-20", items.get(0).vodReleaseDate);
        assertEquals(SHOW, items.get(0).vodSeriesId);
        assertEquals("Show", items.get(0).vodSeriesTitle);
        assertEquals(2, items.get(0).vodSeason);
        assertEquals(3, items.get(0).vodEpisode);
        assertEquals("disneyplus-series:" + SHOW, items.get(1).playUrl);
        assertEquals("vod:disneyplus:series", items.get(1).vodFilterKey);
    }

    @Test public void disneyHasOwnPlatformAndSeriesNavigation() throws Exception {
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path ->
                page(new JSONArray().put(row(path.contains("kind=movie") ? ID : SHOW,
                        path.contains("kind=movie") ? "movie" : "series")), ""));
        List<ChannelItem> items = catalog.catalog();
        MainActivity.VodVisualPlatformFilter disney = MainActivity.VodVisualPlatformFilter.valueOf("DISNEYPLUS");
        assertTrue(MainActivity.matchesVodVisualPlatform(items.get(0), disney));
        assertFalse(MainActivity.matchesVodVisualPlatform(items.get(0), MainActivity.VodVisualPlatformFilter.OTHER));
        assertFalse(MainActivity.matchesVodVisualPlatform(items.get(0), MainActivity.VodVisualPlatformFilter.PRIME));
        assertTrue(MainActivity.isDynamicSeriesGroup(items.get(1)));
        assertFalse(MainActivity.isDynamicSeriesGroup(items.get(0)));
    }

    @Test public void lateResultsRequireSamePermissionsAndSession() {
        OfflinePermissions granted = new OfflinePermissions();
        granted.vodEnabled = true;
        granted.disneyplusVodEnabled = true;
        assertTrue(MainActivity.canApplyDisneyResult(granted, granted, "session-a", "session-a"));
        assertFalse(MainActivity.canApplyDisneyResult(granted, granted, "session-a", "session-b"));
        assertFalse(MainActivity.canApplyDisneyResult(granted, new OfflinePermissions(), "session-a", "session-a"));
        granted.disneyplusVodEnabled = false;
        assertFalse(MainActivity.canApplyDisneyResult(granted, granted, "session-a", "session-a"));
    }

    @Test public void rejectedActiveLoadCanBeRetriedWithoutClearingNewerRequests() {
        DisneyVodLoadState state = new DisneyVodLoadState();
        long old = state.begin(true);
        assertTrue(state.catalogLoading);
        assertTrue(state.finish(old, true, false));
        assertFalse(state.catalogLoading);
        assertFalse(state.catalogLoaded);
        long current = state.begin(true);
        assertFalse(state.finish(old, true, false));
        assertTrue(state.catalogLoading);
        assertTrue(state.finish(current, true, true));
        assertTrue(state.catalogLoaded);
        long episode = state.begin(false);
        assertTrue(state.finish(episode, false, false));
        assertTrue(state.catalogLoaded);
        state.reset();
        assertFalse(state.catalogLoaded);
        assertFalse(state.finish(episode, false, true));
    }

    @Test public void resetRequestsStillDismissTheirOwnedLoadingOverlay() {
        for (boolean catalog : new boolean[]{true, false}) {
            DisneyVodLoadState state = new DisneyVodLoadState();
            long request = state.begin(catalog);
            boolean[] visible = {true};
            state.reset();

            assertFalse(MainActivity.finishDisneyLoad(state, request, catalog, true, 7L, 7L,
                    () -> visible[0] = false));
            assertFalse(visible[0]);
            assertFalse(state.catalogLoading);
            assertFalse(state.catalogLoaded);
        }
    }

    @Test public void staleRequestsPreserveNewerLoadingOverlayAndRequest() {
        DisneyVodLoadState state = new DisneyVodLoadState();
        long old = state.begin(true);
        state.reset();
        long current = state.begin(true);
        boolean[] visible = {true};

        assertFalse(MainActivity.finishDisneyLoad(state, old, true, true, 7L, 8L,
                () -> visible[0] = false));
        assertTrue(visible[0]);
        assertTrue(state.catalogLoading);
        assertFalse(state.catalogLoaded);
        assertTrue(MainActivity.finishDisneyLoad(state, current, true, true, 8L, 8L,
                () -> visible[0] = false));
        assertFalse(visible[0]);
        assertFalse(state.catalogLoading);
        assertTrue(state.catalogLoaded);
    }

    @Test public void rejectedCurrentRequestPreservesUnrelatedLoadingOverlay() {
        DisneyVodLoadState state = new DisneyVodLoadState();
        long request = state.begin(true);
        boolean[] visible = {true};

        assertTrue(MainActivity.finishDisneyLoad(state, request, true, false, 7L, 8L,
                () -> visible[0] = false));
        assertTrue(visible[0]);
        assertFalse(state.catalogLoading);
        assertFalse(state.catalogLoaded);
    }

    @Test public void episodesFollowCursorAndKeepServerOrder() throws Exception {
        List<String> paths = new ArrayList<>();
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path -> {
            paths.add(path);
            if (paths.size() == 1) return page(new JSONArray().put(row(SHOW, "episode")), SHOW);
            return page(new JSONArray().put(row(ID, "episode")), "");
        });
        List<ChannelItem> items = catalog.episodes(SHOW);
        assertEquals(2, items.size());
        assertTrue(paths.get(1).contains("after=" + SHOW));
        assertTrue(paths.get(1).contains("revision=" + REV));
        assertTrue(items.get(0).name.startsWith("T02E03"));
        assertTrue(items.get(0).originalOrder < items.get(1).originalOrder);
    }

    @Test public void changedRevisionRejectsWholeCatalog() {
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path -> {
            JSONObject result = page(new JSONArray(), "");
            if (path.contains("kind=series")) result.put("revision", "ffffffffffffffffffffffffffffffff");
            return result;
        });
        assertThrows(Exception.class, catalog::catalog);
    }

    @Test public void repeatedCursorCannotLoopOrDuplicateRows() {
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path -> page(new JSONArray().put(row(ID, "movie")), ID));
        assertThrows(Exception.class, catalog::catalog);
    }

    @Test public void rejectsOversizedPagesAndUnexpectedEpisodeRows() {
        DisneyVodCatalog catalog = new DisneyVodCatalog("https://example.test", path -> page(new JSONArray().put(row(ID, "episode")), ""));
        assertThrows(Exception.class, catalog::catalog);
        DisneyVodCatalog oversized = new DisneyVodCatalog("https://example.test", path -> {
            JSONArray rows = new JSONArray();
            for (int i = 0; i < 61; i++) rows.put(row(ID, "movie"));
            return page(rows, "");
        });
        assertThrows(Exception.class, oversized::catalog);
    }
}
