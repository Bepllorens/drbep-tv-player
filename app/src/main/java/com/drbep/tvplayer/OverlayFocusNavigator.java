package com.drbep.tvplayer;

final class OverlayFocusNavigator {
    private OverlayFocusNavigator() {
    }

    static int nextSelection(int itemCount, int selectedIndex, int currentIndex, int delta) {
        if (itemCount <= 0) {
            return selectedIndex;
        }
        int anchor = selectedIndex >= 0 && selectedIndex < itemCount
                ? selectedIndex
                : currentIndex >= 0 && currentIndex < itemCount
                ? currentIndex
                : 0;
        int next = anchor + delta;
        if (next < 0) {
            return itemCount - 1;
        }
        if (next >= itemCount) {
            return 0;
        }
        return next;
    }

    static int safeSelection(int itemCount, int selectedIndex, int currentIndex) {
        return nextSelection(itemCount, selectedIndex, currentIndex, 0);
    }
}
