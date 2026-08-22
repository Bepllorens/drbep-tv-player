package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimeshiftUiFactoryTest {
    @Test
    public void touchDragPreviewsCommitsAndAlwaysReleasesDraggingState() {
        FakeHost host = new FakeHost();
        PlayerController.PlaybackSeekState state =
                new PlayerController.PlaybackSeekState(1_000L, 11_000L, 6_000L, "live", true);

        TimeshiftBarUiModel model = TimeshiftUiFactory.build(state, host);

        assertEquals(500, model.progress);
        assertNotNull(model.onSeekStart);
        assertNotNull(model.onSeekEnd);
        model.onSeekStart.run();
        assertTrue(host.dragging);
        assertEquals("preview:3500", model.previewLabelProvider.provide(250));
        model.seekCommitHandler.seekTo(750);
        assertEquals(8_500L, host.seekTargetMs);
        assertFalse(host.dragging);
        assertEquals(1, host.autoHideCalls);

        model.onSeekStart.run();
        model.onSeekEnd.run();
        assertFalse(host.dragging);
    }

    private static final class FakeHost implements TimeshiftUiFactory.Host {
        boolean dragging;
        long seekTargetMs = -1L;
        int autoHideCalls;

        @Override public String statusLabel(PlayerController.PlaybackSeekState state) { return state.label; }
        @Override public String previewLabel(PlayerController.PlaybackSeekState state, long targetMs) { return "preview:" + targetMs; }
        @Override public void showControls() { }
        @Override public boolean resumeLive() { return true; }
        @Override public void showUnavailable() { }
        @Override public void update() { }
        @Override public void markDragging(boolean dragging) { this.dragging = dragging; }
        @Override public void seekTo(long targetMs) { this.seekTargetMs = targetMs; }
        @Override public void scheduleAutoHide() { autoHideCalls++; }
    }
}
