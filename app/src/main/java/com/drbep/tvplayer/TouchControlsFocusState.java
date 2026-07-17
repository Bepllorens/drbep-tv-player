package com.drbep.tvplayer;

final class TouchControlsFocusState {
    interface EnabledActionLookup {
        boolean isEnabled(int index);
    }

    private int actionIndex;
    private boolean timeshiftFocused;

    int actionIndex() {
        return Math.max(0, actionIndex);
    }

    boolean timeshiftFocused() {
        return timeshiftFocused;
    }

    void reset(int firstEnabledActionIndex) {
        timeshiftFocused = false;
        actionIndex = Math.max(0, firstEnabledActionIndex);
    }

    void clear() {
        reset(0);
    }

    void focusTimeshift() {
        timeshiftFocused = true;
    }

    boolean focusActionsIfNeeded() {
        if (!timeshiftFocused) {
            return false;
        }
        timeshiftFocused = false;
        return true;
    }

    void focusAction(int index) {
        timeshiftFocused = false;
        actionIndex = Math.max(0, index);
    }

    boolean moveToNextEnabledAction(int delta, int actionCount, EnabledActionLookup lookup) {
        if (actionCount <= 0 || lookup == null || delta == 0) {
            return false;
        }
        int start = Math.max(0, Math.min(actionCount - 1, actionIndex()));
        int next = start;
        for (int step = 0; step < actionCount; step++) {
            next = (next + delta + actionCount) % actionCount;
            if (lookup.isEnabled(next)) {
                focusAction(next);
                return true;
            }
        }
        return false;
    }
}
