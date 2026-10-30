package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

/** Geometry only: never changes category order, identity or selection. */
final class CategoryPanelSizing {
    private CategoryPanelSizing() {}
    // Keep a bounded sample without sorting the full category catalogue.
    static List<String> longestLabels(List<String> labels) {
        List<String> result = new ArrayList<>(4);
        for (String label : labels) {
            if (label == null) continue;
            int position = 0;
            while (position < result.size() && result.get(position).length() >= label.length()) position++;
            if (position < 4) {
                result.add(position, label);
                if (result.size() > 4) result.remove(4);
            }
        }
        return result;
    }

    static float width(float available, float labelWidth, float titleWidth) {
        if (!Float.isFinite(available) || available <= 0) return 0;
        // TV: preserve most of the carousel. Small windows: retain readable labels.
        float ceiling = Math.min(360f, available * (available < 600f ? .90f : .38f));
        float floor = Math.min(240f, ceiling);
        float label = Float.isFinite(labelWidth) ? Math.max(0, labelWidth) : 0;
        float title = Float.isFinite(titleWidth) ? Math.max(0, titleWidth) : 0;
        return Math.max(floor, Math.min(ceiling, Math.max(label * .60f + 72f, title + 48f)));
    }
}
