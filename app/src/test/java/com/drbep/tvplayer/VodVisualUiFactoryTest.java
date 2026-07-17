package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class VodVisualUiFactoryTest {
    @Test
    public void defaultLibraryLimitsLargeSectionsAndShowsLimitedHint() {
        FakeHost host = new FakeHost();
        host.movistar = vodItems(60);

        VodVisualPanelUiModel model = VodVisualUiFactory.build(
                MainActivity.VodVisualTypeFilter.GENERAL,
                MainActivity.VodVisualPlatformFilter.ALL,
                MainActivity.VodVisualStatusFilter.ALL,
                MainActivity.VodVisualSortFilter.SMART,
                "",
                host
        );

        assertEquals(1, model.sections.size());
        VodVisualSectionUiModel section = model.sections.get(0);
        assertEquals("Movistar (48/60)", section.title);
        assertEquals("Mostrando una muestra. Usa busqueda o filtros para afinar.", section.subtitle);
        assertEquals(48, section.items.size());
    }

    @Test
    public void searchResultsUseLargerButStillBoundedLimit() {
        FakeHost host = new FakeHost();
        host.filteredWithQuery = vodItems(120);

        VodVisualPanelUiModel model = VodVisualUiFactory.build(
                MainActivity.VodVisualTypeFilter.ALL,
                MainActivity.VodVisualPlatformFilter.ALL,
                MainActivity.VodVisualStatusFilter.ALL,
                MainActivity.VodVisualSortFilter.SMART,
                "matrix",
                host
        );

        assertEquals(1, model.sections.size());
        VodVisualSectionUiModel section = model.sections.get(0);
        assertEquals("Resultados (96/120)", section.title);
        assertEquals(96, section.items.size());
        assertFalse(model.actions.isEmpty());
        assertEquals("Editar busqueda", model.actions.get(0).label);
    }

    @Test
    public void smallSectionsDoNotShowLimitedHint() {
        FakeHost host = new FakeHost();
        host.filtered = vodItems(12);

        VodVisualPanelUiModel model = VodVisualUiFactory.build(
                MainActivity.VodVisualTypeFilter.ALL,
                MainActivity.VodVisualPlatformFilter.ALL,
                MainActivity.VodVisualStatusFilter.ALL,
                MainActivity.VodVisualSortFilter.SMART,
                "",
                host
        );

        assertEquals(1, model.sections.size());
        VodVisualSectionUiModel section = model.sections.get(0);
        assertEquals("Resultados (12)", section.title);
        assertEquals("", section.subtitle);
        assertEquals(12, section.items.size());
        assertTrue(section.items.get(0).title.startsWith("VOD "));
    }

    private static List<ChannelItem> vodItems(int count) {
        List<ChannelItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new ChannelItem(
                    String.valueOf(i + 1),
                    "VOD " + (i + 1),
                    "",
                    "https://img.example/" + (i + 1) + ".jpg",
                    "VOD",
                    "https://stream.example/" + (i + 1),
                    "",
                    i,
                    i,
                    true,
                    false,
                    1,
                    "Movistar",
                    Collections.emptyList(),
                    "",
                    "",
                    "vod:movistar",
                    true
            ));
        }
        return items;
    }

    private static final class FakeHost implements VodVisualUiFactory.Host {
        List<ChannelItem> filtered = Collections.emptyList();
        List<ChannelItem> filteredWithQuery = Collections.emptyList();
        List<ChannelItem> movistar = Collections.emptyList();

        @Override
        public String text(int resId) {
            if (resId == R.string.vod_library_continue) return "Continuar";
            if (resId == R.string.vod_library_recent) return "Reciente";
            if (resId == R.string.vod_library_movistar) return "Movistar";
            if (resId == R.string.vod_library_runtime) return "Runtime";
            if (resId == R.string.vod_library_tivify) return "Tivify";
            if (resId == R.string.vod_library_with_progress) return "Con progreso";
            if (resId == R.string.vod_visual_results) return "Resultados";
            if (resId == R.string.vod_visual_section_limited_hint) return "Mostrando una muestra. Usa busqueda o filtros para afinar.";
            if (resId == R.string.vod_visual_filter_edit_search) return "Editar busqueda";
            if (resId == R.string.vod_visual_filter_clear_search) return "Limpiar busqueda";
            if (resId == R.string.vod_visual_filter_search) return "Buscar";
            if (resId == R.string.vod_visual_filter_list_view) return "Vista lista";
            if (resId == R.string.tools_section_vod) return "VOD";
            if (resId == R.string.vod_visual_help) return "Ayuda";
            if (resId == R.string.vod_library_empty) return "Vacio";
            return "res:" + resId;
        }

        @Override
        public String text(int resId, Object... args) {
            if (resId == R.string.vod_visual_section_title_limited) {
                return args[0] + " (" + args[1] + "/" + args[2] + ")";
            }
            if (resId == R.string.vod_visual_section_title) {
                return args[0] + " (" + args[1] + ")";
            }
            if (resId == R.string.vod_visual_filter_type
                    || resId == R.string.vod_visual_filter_platform
                    || resId == R.string.vod_visual_filter_status
                    || resId == R.string.vod_visual_filter_sort) {
                return String.valueOf(args[0]);
            }
            if (resId == R.string.vod_search_results_title) {
                return "Buscar " + args[0];
            }
            return text(resId);
        }

        @Override public boolean protectAdultVod() { return false; }
        @Override public boolean protectedContentLocked() { return false; }
        @Override public boolean defaultFilter(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter) {
            return typeFilter == MainActivity.VodVisualTypeFilter.GENERAL
                    && platformFilter == MainActivity.VodVisualPlatformFilter.ALL
                    && statusFilter == MainActivity.VodVisualStatusFilter.ALL
                    && sortFilter == MainActivity.VodVisualSortFilter.SMART;
        }
        @Override public String librarySummary() { return "Resumen"; }
        @Override public String searchSummary(String query) { return "Buscar " + query; }
        @Override public List<ChannelItem> filteredItems(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter) { return filtered; }
        @Override public List<ChannelItem> filteredItems(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) { return filteredWithQuery; }
        @Override public List<ChannelItem> continueItems() { return Collections.emptyList(); }
        @Override public List<ChannelItem> recentItems() { return Collections.emptyList(); }
        @Override public List<ChannelItem> movistarItems() { return movistar; }
        @Override public List<ChannelItem> runtimeItems() { return Collections.emptyList(); }
        @Override public List<ChannelItem> tivifyItems() { return Collections.emptyList(); }
        @Override public List<ChannelItem> progressItems() { return Collections.emptyList(); }
        @Override public List<ChannelItem> alphaItems() { return Collections.emptyList(); }
        @Override public String displayName(ChannelItem item) { return item == null ? "" : item.name; }
        @Override public String posterMeta(ChannelItem item) { return item == null ? "" : item.platformName; }
        @Override public String protectedTitle(ChannelItem item, String title) { return title; }
        @Override public String protectedMeta(ChannelItem item, String meta) { return meta; }
        @Override public String progressLabel(ChannelItem item) { return ""; }
        @Override public void editSearch(String query) {}
        @Override public void openType(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) {}
        @Override public void openPlatform(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) {}
        @Override public void openStatus(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) {}
        @Override public void openSort(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) {}
        @Override public void clearSearch() {}
        @Override public void openSearch() {}
        @Override public void openListView() {}
        @Override public void unlockAdultAndOpen(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query) {}
        @Override public void openInfo(ChannelItem item) {}
        @Override public void openActions(ChannelItem item) {}
    }
}
