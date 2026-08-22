package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaybackBufferPolicyTest {
    @Test
    public void compactTouchDevicesResumeWithEightSecondBuffer() {
        assertEquals(8_000, PlayerController.bufferForPlaybackAfterRebufferMs(true));
    }

    @Test
    public void tvDevicesKeepSixSecondBuffer() {
        assertEquals(6_000, PlayerController.bufferForPlaybackAfterRebufferMs(false));
    }
}
