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

    @Test
    public void moveToNextEnabledActionSkipsDisabledActionsAndWraps() {
        TouchControlsFocusState state = new TouchControlsFocusState();
        boolean[] enabled = new boolean[] {true, false, false, true};

        state.focusAction(0);

        assertTrue(state.moveToNextEnabledAction(1, enabled.length, index -> enabled[index]));
        assertEquals(3, state.actionIndex());

        assertTrue(state.moveToNextEnabledAction(1, enabled.length, index -> enabled[index]));
        assertEquals(0, state.actionIndex());
    }

    @Test
    public void moveToNextEnabledActionMovesBackwardsAndClearsTimeshiftFocus() {
        TouchControlsFocusState state = new TouchControlsFocusState();
        boolean[] enabled = new boolean[] {true, false, true, false};

        state.focusAction(0);
        state.focusTimeshift();

        assertTrue(state.moveToNextEnabledAction(-1, enabled.length, index -> enabled[index]));
        assertFalse(state.timeshiftFocused());
        assertEquals(2, state.actionIndex());
    }

    @Test
    public void moveToNextEnabledActionKeepsCurrentFocusWhenNothingIsEnabled() {
        TouchControlsFocusState state = new TouchControlsFocusState();
        boolean[] enabled = new boolean[] {false, false, false};

        state.focusAction(2);

        assertFalse(state.moveToNextEnabledAction(1, enabled.length, index -> enabled[index]));
        assertEquals(2, state.actionIndex());
    }
}
