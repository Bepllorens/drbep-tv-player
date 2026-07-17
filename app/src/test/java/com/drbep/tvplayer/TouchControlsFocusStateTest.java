package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TouchControlsFocusStateTest {
    @Test
    public void resetFocusesFirstEnabledActionAndLeavesTimeshift() {
        TouchControlsFocusState state = new TouchControlsFocusState();

        state.focusTimeshift();
        state.reset(3);

        assertFalse(state.timeshiftFocused());
        assertEquals(3, state.actionIndex());
    }

    @Test
    public void focusActionsOnlyChangesStateWhenTimeshiftWasFocused() {
        TouchControlsFocusState state = new TouchControlsFocusState();

        assertFalse(state.focusActionsIfNeeded());

        state.focusTimeshift();

        assertTrue(state.focusActionsIfNeeded());
        assertFalse(state.timeshiftFocused());
    }

    @Test
    public void focusActionClearsTimeshiftFocusAndClampsNegativeIndex() {
        TouchControlsFocusState state = new TouchControlsFocusState();

        state.focusTimeshift();
        state.focusAction(-4);

        assertFalse(state.timeshiftFocused());
        assertEquals(0, state.actionIndex());
    }
}
