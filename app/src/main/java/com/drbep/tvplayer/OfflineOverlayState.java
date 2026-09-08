package com.drbep.tvplayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

final class OfflineOverlayState {
    enum Surface {
        CHANNEL_LIST,
        TOUCH_CONTROLS,
        TIMESHIFT,
        RECORDINGS,
        MULTIVIEW,
        STARTUP_LOADING,
        ZAP_BANNER
    }

    private final Set<Surface> visibleSurfaces = EnumSet.noneOf(Surface.class);
    private final Deque<Surface> focusHistory = new ArrayDeque<>();
    private Surface focusedSurface;

    void setVisible(Surface surface, boolean visible) {
        setVisible(surface, visible, visible);
    }

    void setVisible(Surface surface, boolean visible, boolean takeFocus) {
        if (surface == null) {
            return;
        }
        if (visible) {
            visibleSurfaces.add(surface);
            if (takeFocus) {
                rememberFocus(surface);
            }
            return;
        }
        visibleSurfaces.remove(surface);
        focusHistory.remove(surface);
        if (focusedSurface == surface) {
            restoreMostRecentFocus();
        }
    }

    void focusSurface(Surface surface) {
        if (surface != null && visibleSurfaces.contains(surface)) {
            rememberFocus(surface);
        }
    }

    boolean isVisible(Surface surface) {
        return surface != null && visibleSurfaces.contains(surface);
    }

    boolean hasBlockingPanelVisible() {
        return isVisible(Surface.CHANNEL_LIST)
                || isVisible(Surface.RECORDINGS)
                || isVisible(Surface.MULTIVIEW);
    }

    Surface focusedSurface() {
        return focusedSurface;
    }

    void clearTransientPlaybackSurfaces() {
        setVisible(Surface.TOUCH_CONTROLS, false);
        setVisible(Surface.TIMESHIFT, false);
        setVisible(Surface.ZAP_BANNER, false);
    }

    void reset() {
        visibleSurfaces.clear();
        focusHistory.clear();
        focusedSurface = null;
    }

    private void rememberFocus(Surface surface) {
        focusHistory.remove(surface);
        focusHistory.addLast(surface);
        focusedSurface = surface;
    }

    private void restoreMostRecentFocus() {
        Iterator<Surface> iterator = focusHistory.descendingIterator();
        while (iterator.hasNext()) {
            Surface candidate = iterator.next();
            if (visibleSurfaces.contains(candidate)) {
                focusedSurface = candidate;
                return;
            }
            iterator.remove();
        }
        focusedSurface = null;
    }
}
