package com.drbep.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ChannelOverlayCoordinator {
    private final List<ChannelItem> channels;
    private final List<ChannelItem> allChannels;
    private final List<ChannelFilter> filters;
    private final Set<String> favoriteChannelIds;

    private int currentIndex;
    private int selectedOverlayIndex;
    private boolean favoritesOnly;
    private String selectedFilterKey;

    ChannelOverlayCoordinator(List<ChannelItem> channels, List<ChannelItem> allChannels, List<ChannelFilter> filters, Set<String> favoriteChannelIds) {
        this.channels = channels;
        this.allChannels = allChannels;
        this.filters = filters;
        this.favoriteChannelIds = favoriteChannelIds;
        this.currentIndex = -1;
        this.selectedOverlayIndex = 0;
        this.favoritesOnly = false;
        this.selectedFilterKey = "all";
    }

    void syncState(int currentIndex, int selectedOverlayIndex, boolean favoritesOnly, String selectedFilterKey) {
        this.currentIndex = currentIndex;
        this.selectedOverlayIndex = selectedOverlayIndex;
        this.favoritesOnly = favoritesOnly;
        this.selectedFilterKey = selectedFilterKey;
    }

    int getCurrentIndex() {
        return currentIndex;
    }

    int getSelectedOverlayIndex() {
        return selectedOverlayIndex;
    }

    boolean isFavoritesOnly() {
        return favoritesOnly;
    }

    String getSelectedFilterKey() {
        return selectedFilterKey;
    }

    void applyLoadedChannels(CatalogLoadResult result, String lastChannelId) {
        allChannels.clear();
        allChannels.addAll(result.channels);

        filters.clear();
        filters.addAll(result.filters);

        int foundFilterIndex = findFilterIndexByKey(selectedFilterKey);
        if (foundFilterIndex < 0) {
            foundFilterIndex = findFilterIndexByKey(result.defaultFilterKey);
        }
        if (foundFilterIndex < 0) {
            foundFilterIndex = 0;
        }
        selectedFilterKey = filters.isEmpty() ? "all" : filters.get(foundFilterIndex).key;

        rebuildVisibleChannels(lastChannelId, lastChannelId);
    }

    void moveOverlaySelection(int delta) {
        if (channels.isEmpty()) {
            return;
        }
        if (selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            selectedOverlayIndex = currentIndex >= 0 ? currentIndex : 0;
        }
        selectedOverlayIndex += delta;
        if (selectedOverlayIndex < 0) {
            selectedOverlayIndex = channels.size() - 1;
        }
        if (selectedOverlayIndex >= channels.size()) {
            selectedOverlayIndex = 0;
        }
    }

    ChannelFilter cycleFilter(int delta) {
        if (filters.isEmpty()) {
            return null;
        }
        int currentFilterIndex = findFilterIndexByKey(selectedFilterKey);
        if (currentFilterIndex < 0) {
            currentFilterIndex = 0;
        }

        int next = currentFilterIndex + delta;
        if (next < 0) {
            next = filters.size() - 1;
        }
        if (next >= filters.size()) {
            next = 0;
        }

        selectedFilterKey = filters.get(next).key;

        String keepCurrentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";
        String keepSelectedID = (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) ? channels.get(selectedOverlayIndex).id : keepCurrentID;
        rebuildVisibleChannels(keepCurrentID, keepSelectedID);
        return getSelectedFilter();
    }

    boolean toggleFavoriteSelected() {
        if (channels.isEmpty() || selectedOverlayIndex < 0 || selectedOverlayIndex >= channels.size()) {
            return false;
        }

        String selectedID = channels.get(selectedOverlayIndex).id;
        String currentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";
        boolean added;
        if (favoriteChannelIds.contains(selectedID)) {
            favoriteChannelIds.remove(selectedID);
            added = false;
        } else {
            favoriteChannelIds.add(selectedID);
            added = true;
        }

        rebuildVisibleChannels(currentID, selectedID);
        return added;
    }

    boolean toggleFavoritesOnlyMode() {
        String currentID = (currentIndex >= 0 && currentIndex < channels.size()) ? channels.get(currentIndex).id : "";
        String selectedID = (selectedOverlayIndex >= 0 && selectedOverlayIndex < channels.size()) ? channels.get(selectedOverlayIndex).id : currentID;

        favoritesOnly = !favoritesOnly;
        rebuildVisibleChannels(currentID, selectedID);
        return favoritesOnly;
    }

    void updateFilterText(TextView filterText, Context context) {
        if (filterText == null) {
            return;
        }
        ChannelFilter filter = getSelectedFilter();
        if (filter == null) {
            filterText.setText(context.getString(R.string.filter_all_label));
            return;
        }
        filterText.setText(context.getString(R.string.status_filter_changed, filter.label));
    }

    boolean isOverlayVisible(View overlayView) {
        return overlayView != null && overlayView.getVisibility() == View.VISIBLE;
    }

    void showOverlay(View overlayView, Handler uiHandler, Runnable hideOverlayRunnable, long overlayHideMs) {
        if (overlayView == null) {
            return;
        }
        overlayView.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideOverlayRunnable);
        uiHandler.postDelayed(hideOverlayRunnable, overlayHideMs);
    }

    void hideOverlay(View overlayView) {
        if (overlayView == null) {
            return;
        }
        overlayView.setVisibility(View.GONE);
    }

    int findChannelIndexById(String channelID) {
        if (channelID == null || channelID.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < channels.size(); i++) {
            if (channelID.equals(channels.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    private void applyFavoritesAndSort(List<ChannelItem> target) {
        for (ChannelItem item : target) {
            item.favorite = favoriteChannelIds.contains(item.id);
        }
        target.sort((a, b) -> {
            int byDashboardOrder = Integer.compare(a.dashboardOrder, b.dashboardOrder);
            if (byDashboardOrder != 0) {
                return byDashboardOrder;
            }
            return Integer.compare(a.originalOrder, b.originalOrder);
        });
    }

    private void rebuildVisibleChannels(String keepCurrentChannelId, String keepSelectedChannelId) {
        applyFavoritesAndSort(allChannels);

        channels.clear();
        for (ChannelItem item : allChannels) {
            if (!channelMatchesCurrentFilter(item)) {
                continue;
            }
            if (!favoritesOnly || item.favorite) {
                channels.add(item);
            }
        }

        currentIndex = findChannelIndexById(keepCurrentChannelId);
        selectedOverlayIndex = findChannelIndexById(keepSelectedChannelId);
        if (selectedOverlayIndex < 0 && currentIndex >= 0) {
            selectedOverlayIndex = currentIndex;
        }
        if (selectedOverlayIndex < 0 && !channels.isEmpty()) {
            selectedOverlayIndex = 0;
        }
    }

    private boolean channelMatchesCurrentFilter(ChannelItem item) {
        ChannelFilter filter = getSelectedFilter();
        if (filter == null || filter.type == 0) {
            return true;
        }
        if (filter.type == 1) {
            return item.platformId == filter.platformId && !item.isVod;
        }
        if (filter.type == 2) {
            for (String name : item.customGroups) {
                if (name != null && name.equalsIgnoreCase(filter.groupName)) {
                    return true;
                }
            }
            return false;
        }
        if (filter.type == 3) {
            return item.isVod;
        }
        return true;
    }

    private ChannelFilter getSelectedFilter() {
        for (ChannelFilter filter : filters) {
            if (filter.key.equals(selectedFilterKey)) {
                return filter;
            }
        }
        return filters.isEmpty() ? null : filters.get(0);
    }

    private int findFilterIndexByKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < filters.size(); i++) {
            if (key.equals(filters.get(i).key)) {
                return i;
            }
        }
        return -1;
    }
}