package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RecordingsControllerTest {
    @Test
    public void applyResultSelectsPreferredRecordingWhenPresent() {
        RecordingsController controller = new RecordingsController();

        controller.applyResult(result(false, item("a", true, "completed"), item("b", true, "completed")), "b");

        assertEquals("b", controller.getSelectedItem().id);
        assertEquals(1, controller.getSelectedIndex());
    }

    @Test
    public void moveSelectionWrapsAround() {
        RecordingsController controller = new RecordingsController();
        controller.applyResult(result(false, item("a", true, "completed"), item("b", true, "completed")), null);

        controller.moveSelection(-1);

        assertEquals("b", controller.getSelectedItem().id);
        assertEquals(1, controller.getSelectedIndex());
    }

    @Test
    public void clearCurrentResultRemembersSelectedId() {
        RecordingsController controller = new RecordingsController();
        controller.applyResult(result(false, item("a", true, "completed"), item("b", true, "completed")), "b");

        controller.clearCurrentResult();

        assertNull(controller.getSelectedItem());
        assertEquals("b", controller.getLastSelectedId());
        assertEquals(0, controller.getSelectedIndex());
    }

    @Test
    public void summaryStatsClassifiesScheduledRecordingAndIssueStates() {
        RecordingsController.SummaryStats stats = RecordingsController.buildSummaryStats(result(
                true,
                item("a", false, "scheduled"),
                item("b", false, "running"),
                item("c", false, "failed"),
                item("d", false, "cancelled")
        ));

        assertEquals(4, stats.total);
        assertEquals(1, stats.scheduled);
        assertEquals(1, stats.recording);
        assertEquals(2, stats.issue);
        assertEquals(0, stats.conflict);
    }

    @Test
    public void summaryStatsCountsOverlappingScheduledConflicts() {
        RecordingsController.SummaryStats stats = RecordingsController.buildSummaryStats(result(
                true,
                item("a", false, "scheduled", "2026-05-02T10:00:00+02:00", "2026-05-02T11:00:00+02:00"),
                item("b", false, "scheduled", "2026-05-02T10:30:00+02:00", "2026-05-02T12:00:00+02:00"),
                item("c", false, "scheduled", "2026-05-02T12:30:00+02:00", "2026-05-02T13:00:00+02:00")
        ));

        assertEquals(1, stats.conflict);
    }

    private static RecordingsRepository.RecordingsResult result(boolean scheduledMode, RecordingsRepository.RecordingItem... items) {
        return new RecordingsRepository.RecordingsResult(
                "",
                items == null ? Collections.emptyList() : Arrays.asList(items),
                scheduledMode
        );
    }

    private static RecordingsRepository.RecordingItem item(String id, boolean playable, String status) {
        return item(id, playable, status, "", "");
    }

    private static RecordingsRepository.RecordingItem item(String id, boolean playable, String status, String startTime, String endTime) {
        return new RecordingsRepository.RecordingItem(
                id,
                "Recording " + id,
                id + ".mp4",
                0L,
                "",
                "Channel",
                "Program",
                "",
                status,
                startTime,
                endTime,
                playable
        );
    }
}
