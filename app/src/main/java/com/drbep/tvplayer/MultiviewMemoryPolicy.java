package com.drbep.tvplayer;

import android.content.ComponentCallbacks2;

/** UI-hidden is a lifecycle notification, not critical memory pressure. */
final class MultiviewMemoryPolicy {
    private MultiviewMemoryPolicy() {}

    static boolean shouldClose(int level) {
        return level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                || (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
                && level < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN);
    }
}
