package com.drbep.tvplayer;

final class OverlayNavigationState {
    int currentIndex = -1;
    int selectedOverlayIndex = 0;
    boolean favoritesOnly;
    String selectedFilterKey = "all";

    void reset() {
        currentIndex = -1;
        selectedOverlayIndex = 0;
        favoritesOnly = false;
        selectedFilterKey = "all";
    }
}
