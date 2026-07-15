package com.drbep.tvplayer;

import java.util.EnumSet;
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
    private Surface focusedSurface;

    void setVisible(Surface surface, boolean visible) {
        if (surface == null) {
            return;
        }
        if (visible) {
            visibleSurfaces.add(surface);
            focusedSurface = surface;
            return;
        }
        visibleSurfaces.remove(surface);
        if (focusedSurface == surface) {
            focusedSurface = visibleSurfaces.isEmpty() ? null : visibleSurfaces.iterator().next();
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
        focusedSurface = null;
    }
}
