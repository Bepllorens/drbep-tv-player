package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class VodLibraryMenuUiFactory {
    interface Host {
        String text(int resId);
        String optionLabel(int titleResId, List<ChannelItem> items);
        String protectedLabel(String label, boolean protectedEntry);
        boolean protectAdultVod();
        void openContinue();
        void openRecent();
        void openTivify();
        void openTivifyAdult();
        void openRuntime();
        void openProgress();
        void openNotStarted();
        void openCategories();
        void openAllAlpha();
        void openSortYear();
        void openSortDuration();
        void openSearch();
        void openProgressManager();
    }

    private VodLibraryMenuUiFactory() {
    }

    static TvOptionsMenuModel build(
            List<ChannelItem> continueItems,
            List<ChannelItem> recentItems,
            List<ChannelItem> tivifyItems,
            List<ChannelItem> tivifyAdultItems,
            List<ChannelItem> runtimeItems,
            List<ChannelItem> progressItems,
            List<ChannelItem> notStartedItems,
            List<ChannelItem> allVodItems,
            Host host
    ) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.optionLabel(R.string.vod_library_continue, continueItems), host::openContinue);
        add(options, actions, host.optionLabel(R.string.vod_library_recent, recentItems), host::openRecent);
        add(options, actions, host.optionLabel(R.string.vod_library_tivify, tivifyItems), host::openTivify);
        add(options, actions, host.protectedLabel(host.optionLabel(R.string.vod_library_tivify_adult, tivifyAdultItems), host.protectAdultVod()), host::openTivifyAdult);
        add(options, actions, host.optionLabel(R.string.vod_library_runtime, runtimeItems), host::openRuntime);
        add(options, actions, host.optionLabel(R.string.vod_library_with_progress, progressItems), host::openProgress);
        add(options, actions, host.optionLabel(R.string.vod_library_not_started, notStartedItems), host::openNotStarted);
        add(options, actions, host.text(R.string.vod_library_categories), host::openCategories);
        add(options, actions, host.optionLabel(R.string.vod_library_all_alpha, allVodItems), host::openAllAlpha);
        add(options, actions, host.text(R.string.vod_library_sort_year), host::openSortYear);
        add(options, actions, host.text(R.string.vod_library_sort_duration), host::openSortDuration);
        add(options, actions, host.text(R.string.quick_hub_search_vod), host::openSearch);
        add(options, actions, host.optionLabel(R.string.vod_library_manage_progress, progressItems), host::openProgressManager);
        return new TvOptionsMenuModel(options, actions);
    }

    private static void add(List<String> options, List<Runnable> actions, String label, Runnable action) {
        options.add(label);
        actions.add(action);
    }
}
