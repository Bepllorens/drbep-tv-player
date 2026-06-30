package com.drbep.tvplayer;

import java.util.List;

final class ZapBannerState {
    private boolean visible;
    private int selectedActionIndex;

    boolean isVisible() {
        return visible;
    }

    void show() {
        if (!visible) {
            selectedActionIndex = 0;
        }
        visible = true;
    }

    void hide() {
        visible = false;
        selectedActionIndex = 0;
    }

    int getSelectedActionIndex() {
        return selectedActionIndex;
    }

    void ensureValidSelection(List<ZapActionItem> actionItems) {
        if (!visible || actionItems == null || actionItems.isEmpty()) {
            selectedActionIndex = 0;
            return;
        }
        int clamped = Math.max(0, Math.min(selectedActionIndex, actionItems.size() - 1));
        ZapActionItem current = actionItems.get(clamped);
        if (current != null && current.enabled) {
            selectedActionIndex = clamped;
            return;
        }
        for (int i = clamped; i < actionItems.size(); i++) {
            ZapActionItem candidate = actionItems.get(i);
            if (candidate != null && candidate.enabled) {
                selectedActionIndex = i;
                return;
            }
        }
        for (int i = clamped - 1; i >= 0; i--) {
            ZapActionItem candidate = actionItems.get(i);
            if (candidate != null && candidate.enabled) {
                selectedActionIndex = i;
                return;
            }
        }
        selectedActionIndex = 0;
    }

    boolean moveSelection(int delta, List<ZapActionItem> actionItems) {
        if (!visible || actionItems == null || actionItems.isEmpty()) {
            return false;
        }
        int direction = delta >= 0 ? 1 : -1;
        int length = actionItems.size();
        int start = Math.max(0, Math.min(selectedActionIndex, length - 1));
        for (int candidate = start + direction; candidate >= 0 && candidate < length; candidate += direction) {
            ZapActionItem item = actionItems.get(candidate);
            if (item != null && item.enabled) {
                selectedActionIndex = candidate;
                return true;
            }
        }
        return false;
    }

    ZapActionItem getSelectedAction(List<ZapActionItem> actionItems) {
        if (!visible || actionItems == null || selectedActionIndex < 0 || selectedActionIndex >= actionItems.size()) {
            return null;
        }
        return actionItems.get(selectedActionIndex);
    }
}
