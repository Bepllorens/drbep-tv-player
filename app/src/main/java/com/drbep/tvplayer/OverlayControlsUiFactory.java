package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class OverlayControlsUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String quickCountLabel(int labelRes, int count);
        String currentFilterLabel();
        String searchQuery();
        int searchFocusRequestToken();
        int searchClearFocusRequestToken();
        boolean tvActive();
        boolean vodActive();
        boolean adultActive();
        boolean showVodTarget();
        boolean showAdultTarget();
        boolean adultTargetProtected();
        boolean recordingsEnabled();
        boolean recordingsVisible();
        boolean favoritesSelected();
        int countForTarget(String targetKey);
        int recentCount();
        int favoriteCount();
        void keepVisible();
        void cycleFilter(int delta);
        void focusSearch();
        void applyQuickTarget(String targetKey);
        void openRecordings();
        void openRecentChannels();
        void openFavoriteChannels();
        void toggleFavoritesOnly();
        void applySearchQuery(String query);
        void onSearchFocused();
        String decorateProtectedLabel(String label, boolean locked);
    }

    private OverlayControlsUiFactory() {
    }

    static OverlayControlsUiModel build(Host host) {
        if (host == null) {
            return new OverlayControlsUiModel("", "", "", "", 0, 0, null, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<ZapActionItem> filterActions = new ArrayList<>();
        filterActions.add(new ZapActionItem(host.text(R.string.filter_prev_button), true, false, false, () -> {
            host.keepVisible();
            host.cycleFilter(-1);
        }));
        filterActions.add(new ZapActionItem(host.currentFilterLabel(), true, false, false, () -> {
            host.keepVisible();
            host.cycleFilter(1);
        }, () -> {
            host.keepVisible();
            host.cycleFilter(-1);
        }));
        filterActions.add(new ZapActionItem(host.text(R.string.filter_search_button), true, false, false, () -> {
            host.keepVisible();
            host.focusSearch();
        }));
        filterActions.add(new ZapActionItem(host.text(R.string.filter_next_button), true, false, false, () -> {
            host.keepVisible();
            host.cycleFilter(1);
        }));

        List<ZapActionItem> primaryActions = new ArrayList<>();
        primaryActions.add(new ZapActionItem(
                host.quickCountLabel(R.string.overlay_quick_tv, host.countForTarget("tv")),
                true,
                false,
                host.tvActive(),
                () -> host.applyQuickTarget("tv")
        ));
        if (host.showVodTarget()) {
            primaryActions.add(new ZapActionItem(
                    host.quickCountLabel(R.string.overlay_quick_vod, host.countForTarget("vod")),
                    true,
                    false,
                    host.vodActive(),
                    () -> host.applyQuickTarget("vod")
            ));
        }
        if (host.showAdultTarget()) {
            boolean protectedTarget = host.adultTargetProtected();
            primaryActions.add(new ZapActionItem(
                    host.decorateProtectedLabel(host.quickCountLabel(R.string.overlay_quick_adult, host.countForTarget("vod-adult")), protectedTarget),
                    true,
                    protectedTarget,
                    host.adultActive(),
                    () -> host.applyQuickTarget("vod-adult")
            ));
        }
        if (host.recordingsEnabled()) {
            primaryActions.add(new ZapActionItem(host.text(R.string.overlay_quick_grab), true, false, host.recordingsVisible(), () -> {
                host.keepVisible();
                host.openRecordings();
            }));
        }

        List<ZapActionItem> secondaryActions = new ArrayList<>();
        secondaryActions.add(new ZapActionItem(host.text(R.string.overlay_recent_button_count, host.recentCount()), true, false, false, () -> {
            host.keepVisible();
            host.openRecentChannels();
        }));
        secondaryActions.add(new ZapActionItem(
                host.text(host.favoritesSelected() ? R.string.overlay_favorites_button_on_count : R.string.overlay_favorites_button_off_count, host.favoriteCount()),
                true,
                false,
                host.favoritesSelected(),
                () -> {
                    host.keepVisible();
                    host.openFavoriteChannels();
                },
                () -> {
                    host.keepVisible();
                    host.toggleFavoritesOnly();
                }
        ));

        return new OverlayControlsUiModel(
                host.text(R.string.filter_navigation_hint),
                host.currentFilterLabel(),
                host.text(R.string.overlay_search_hint),
                host.searchQuery(),
                host.searchFocusRequestToken(),
                host.searchClearFocusRequestToken(),
                host::applySearchQuery,
                () -> {
                    host.keepVisible();
                    host.onSearchFocused();
                },
                filterActions,
                primaryActions,
                secondaryActions
        );
    }
}
