package com.drbep.tvplayer;

final class TouchControlsFocusState {
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
}
