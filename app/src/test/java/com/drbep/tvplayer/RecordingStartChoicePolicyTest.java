package com.drbep.tvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecordingStartChoicePolicyTest {
    @Test public void offersChoiceOnlyForAlreadyStartedProgram() {
        assertTrue(RecordingStartChoicePolicy.isInProgress(100, 300, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(200, 300, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(250, 300, 200));
    }
    @Test public void endedOrInvalidProgramsCannotOfferStartOver() {
        assertFalse(RecordingStartChoicePolicy.isInProgress(100, 200, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(100, 150, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(0, 300, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(-1, 300, 200));
        assertFalse(RecordingStartChoicePolicy.isInProgress(300, 100, 200));
    }
}
