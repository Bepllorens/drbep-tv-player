package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class VodVisualUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        boolean protectAdultVod();
        boolean protectedContentLocked();
        boolean defaultFilter(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter);
        String librarySummary();
        String searchSummary(String query);
        List<ChannelItem> filteredItems(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter);
        List<ChannelItem> filteredItems(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        List<ChannelItem> continueItems();
        List<ChannelItem> recentItems();
        List<ChannelItem> runtimeItems();
        List<ChannelItem> tivifyItems();
        List<ChannelItem> progressItems();
        List<ChannelItem> alphaItems();
        String displayName(ChannelItem item);
        String posterMeta(ChannelItem item);
        String protectedTitle(ChannelItem item, String title);
        String protectedMeta(ChannelItem item, String meta);
        String progressLabel(ChannelItem item);
        void editSearch(String query);
        void openType(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        void openPlatform(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        void openStatus(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        void openSort(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        void clearSearch();
        void openSearch();
        void openListView();
        void unlockAdultAndOpen(MainActivity.VodVisualTypeFilter typeFilter, MainActivity.VodVisualPlatformFilter platformFilter, MainActivity.VodVisualStatusFilter statusFilter, MainActivity.VodVisualSortFilter sortFilter, String query);
        void openInfo(ChannelItem item);
        void openActions(ChannelItem item);
    }

    private VodVisualUiFactory() {
    }

    static VodVisualPanelUiModel build(
            MainActivity.VodVisualTypeFilter typeFilter,
            MainActivity.VodVisualPlatformFilter platformFilter,
            MainActivity.VodVisualStatusFilter statusFilter,
            MainActivity.VodVisualSortFilter sortFilter,
            String query,
            Host host
    ) {
        String trimmedQuery = query == null ? "" : query.trim();
        boolean searchMode = !trimmedQuery.isEmpty();
        List<VodVisualActionUiModel> actions = buildActions(typeFilter, platformFilter, statusFilter, sortFilter, trimmedQuery, searchMode, host);
        List<VodVisualSectionUiModel> sections = buildSections(typeFilter, platformFilter, statusFilter, sortFilter, trimmedQuery, searchMode, host);
        return new VodVisualPanelUiModel(
                searchMode ? host.text(R.string.vod_search_results_title, trimmedQuery) : host.text(R.string.tools_section_vod),
                searchMode ? host.searchSummary(trimmedQuery) : host.librarySummary(),
                host.text(R.string.vod_visual_help),
                host.text(R.string.vod_library_empty),
                actions,
                sections
        );
    }

    private static List<VodVisualActionUiModel> buildActions(
            MainActivity.VodVisualTypeFilter typeFilter,
            MainActivity.VodVisualPlatformFilter platformFilter,
            MainActivity.VodVisualStatusFilter statusFilter,
            MainActivity.VodVisualSortFilter sortFilter,
            String query,
            boolean searchMode,
            Host host
    ) {
        List<VodVisualActionUiModel> actions = new ArrayList<>();
        if (searchMode) {
            actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_edit_search), false, () -> host.editSearch(query)));
        }
        actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_type, typeFilter.label), true, () -> {
            MainActivity.VodVisualTypeFilter nextType = typeFilter.next();
            if (nextType == MainActivity.VodVisualTypeFilter.ADULT && host.protectAdultVod() && host.protectedContentLocked()) {
                host.unlockAdultAndOpen(nextType, platformFilter, statusFilter, sortFilter, query);
                return;
            }
            host.openType(nextType, platformFilter, statusFilter, sortFilter, query);
        }));
        actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_platform, platformFilter.label), true, () -> host.openPlatform(typeFilter, platformFilter.next(), statusFilter, sortFilter, query)));
        actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_status, statusFilter.label), true, () -> host.openStatus(typeFilter, platformFilter, statusFilter.next(), sortFilter, query)));
        actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_sort, sortFilter.label), true, () -> host.openSort(typeFilter, platformFilter, statusFilter, sortFilter.next(), query)));
        if (searchMode) {
            actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_clear_search), false, host::clearSearch));
        } else {
            actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_search), false, host::openSearch));
            actions.add(new VodVisualActionUiModel(host.text(R.string.vod_visual_filter_list_view), false, host::openListView));
        }
        return actions;
    }

    private static List<VodVisualSectionUiModel> buildSections(
            MainActivity.VodVisualTypeFilter typeFilter,
            MainActivity.VodVisualPlatformFilter platformFilter,
            MainActivity.VodVisualStatusFilter statusFilter,
            MainActivity.VodVisualSortFilter sortFilter,
            String query,
            boolean searchMode,
            Host host
    ) {
        List<VodVisualSectionUiModel> sections = new ArrayList<>();
        if (searchMode) {
            addSection(sections, host.text(R.string.vod_visual_results), host.filteredItems(typeFilter, platformFilter, statusFilter, sortFilter, query), host);
        } else if (host.defaultFilter(typeFilter, platformFilter, statusFilter, sortFilter)) {
            addSection(sections, host.text(R.string.vod_library_continue), host.continueItems(), host);
            addSection(sections, host.text(R.string.vod_library_recent), host.recentItems(), host);
            addSection(sections, host.text(R.string.vod_library_runtime), host.runtimeItems(), host);
            addSection(sections, host.text(R.string.vod_library_tivify), host.tivifyItems(), host);
            addSection(sections, host.text(R.string.vod_library_with_progress), host.progressItems(), host);
            addSection(sections, host.text(R.string.vod_library_all_alpha), host.alphaItems(), host);
        } else {
            addSection(sections, host.text(R.string.vod_visual_results), host.filteredItems(typeFilter, platformFilter, statusFilter, sortFilter), host);
        }
        return sections;
    }

    private static void addSection(List<VodVisualSectionUiModel> sections, String title, List<ChannelItem> items, Host host) {
        if (sections == null || items == null || items.isEmpty()) {
            return;
        }
        List<VodVisualItemUiModel> mapped = new ArrayList<>();
        for (ChannelItem item : items) {
            if (item == null) {
                continue;
            }
            mapped.add(new VodVisualItemUiModel(
                    host.protectedTitle(item, host.displayName(item)),
                    host.protectedMeta(item, host.posterMeta(item)),
                    host.progressLabel(item),
                    item.logoUrl,
                    () -> host.openInfo(item),
                    () -> host.openActions(item)
            ));
        }
        if (!mapped.isEmpty()) {
            sections.add(new VodVisualSectionUiModel(host.text(R.string.vod_visual_section_title, title, mapped.size()), mapped));
        }
    }
}
