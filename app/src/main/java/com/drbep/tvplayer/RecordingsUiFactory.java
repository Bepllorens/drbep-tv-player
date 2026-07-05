package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class RecordingsUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String summary(RecordingsRepository.RecordingsResult result);
        String hint();
        String title(RecordingsRepository.RecordingItem item);
        String meta(RecordingsRepository.RecordingItem item);
        int metaColor(RecordingsRepository.RecordingItem item);
        String statusLabel(RecordingsRepository.RecordingItem item);
        int statusBadgeColor(RecordingsRepository.RecordingItem item);
        RecordingsRepository.RecordingsResult currentResult();
        RecordingsRepository.RecordingItem selectedItem();
        int selectedIndex();
        boolean scheduledMode();
        boolean headerFocusActive();
        int headerFocusIndex();
        int pendingScrollIndex();
        void switchMode(boolean scheduledMode);
        void refresh();
        void selectAndPlay(int position, RecordingsRepository.RecordingItem item, String basePath);
    }

    interface PresentationHost {
        String text(int resId);
        String text(int resId, Object... args);
        String safeLower(String value);
        String filterLabel();
        boolean hasConflict(RecordingsRepository.RecordingItem item, RecordingsRepository.RecordingsResult result);
    }

    private RecordingsUiFactory() {
    }

    static RecordingsSurfaceUiModel build(Host host) {
        if (host == null) {
            return new RecordingsSurfaceUiModel(
                    new RecordingsPanelUiModel("", "", false, -1, null, null, null, "", "", "", "", 0xFFF2D5AF, "", false, ""),
                    new RecordingListUiModel(new ArrayList<>(), -1)
            );
        }
        return new RecordingsSurfaceUiModel(buildPanel(host), buildList(host));
    }

    private static RecordingsPanelUiModel buildPanel(Host host) {
        RecordingsRepository.RecordingsResult result = host.currentResult();
        boolean scheduledMode = host.scheduledMode();
        String sectionTitle = host.text(scheduledMode ? R.string.title_recordings_scheduled : R.string.title_recordings_completed);
        String detailTitle = host.text(R.string.recordings_detail_empty);
        String detailMeta = "";
        int detailMetaColor = 0xFFF2D5AF;
        String detailPath = "";
        boolean detailPathVisible = false;
        String detailAction = host.text(scheduledMode ? R.string.recordings_panel_action_hint_scheduled : R.string.recordings_panel_action_hint);
        String posterUrl = "";

        if (result != null && result.items != null && !result.items.isEmpty()) {
            RecordingsRepository.RecordingItem item = host.selectedItem();
            if (item != null) {
                detailTitle = host.title(item);
                detailMeta = host.meta(item);
                detailMetaColor = host.metaColor(item);
                if (item.playable) {
                    detailPathVisible = true;
                    detailPath = host.text(R.string.recordings_path, item.path == null ? "" : item.path);
                }
                posterUrl = item.poster == null ? "" : item.poster;
            }
        }

        return new RecordingsPanelUiModel(
                sectionTitle,
                host.summary(result),
                scheduledMode,
                host.headerFocusActive() ? host.headerFocusIndex() : -1,
                () -> host.switchMode(false),
                () -> host.switchMode(true),
                host::refresh,
                host.hint(),
                posterUrl,
                detailTitle,
                detailMeta,
                detailMetaColor,
                detailPath,
                detailPathVisible,
                detailAction
        );
    }

    private static RecordingListUiModel buildList(Host host) {
        RecordingsRepository.RecordingsResult result = host.currentResult();
        List<RecordingListRowUiModel> items = new ArrayList<>();
        if (result != null && result.items != null) {
            for (int i = 0; i < result.items.size(); i++) {
                final int position = i;
                final RecordingsRepository.RecordingItem item = result.items.get(i);
                final String basePath = result.basePath;
                items.add(new RecordingListRowUiModel(
                        host.title(item),
                        host.meta(item),
                        host.metaColor(item),
                        host.statusLabel(item),
                        host.statusBadgeColor(item),
                        item == null ? "" : item.poster,
                        position == host.selectedIndex(),
                        () -> host.selectAndPlay(position, item, basePath)
                ));
            }
        }
        return new RecordingListUiModel(items, host.pendingScrollIndex());
    }

    static String buildSummary(RecordingsRepository.RecordingsResult result, PresentationHost host) {
        String summary;
        if (result == null || result.items == null || result.items.isEmpty()) {
            summary = result != null && result.scheduledMode
                    ? host.text(R.string.recordings_summary_scheduled, 0, 0, 0, 0, 0)
                    : host.text(R.string.recordings_summary_completed, 0);
        } else if (!result.scheduledMode) {
            summary = host.text(R.string.recordings_summary_completed, result.items.size());
        } else {
            RecordingsController.SummaryStats stats = RecordingsController.buildSummaryStats(result);
            summary = host.text(R.string.recordings_summary_scheduled, stats.total, stats.scheduled, stats.recording, stats.issue, stats.conflict);
        }
        String label = host.filterLabel();
        return label == null || label.isEmpty() ? summary : summary + "  ·  " + label;
    }

    static String statusLabel(RecordingsRepository.RecordingItem item, RecordingsRepository.RecordingsResult result, PresentationHost host) {
        if (item == null || item.status == null || item.status.trim().isEmpty()) {
            return host.text(R.string.recording_status_ready);
        }
        if (host.hasConflict(item, result)) {
            return host.text(R.string.recording_status_conflict_short);
        }
        String status = item.status.trim().toLowerCase(java.util.Locale.US);
        switch (status) {
            case "completed":
                return host.text(R.string.recording_status_completed_short);
            case "recording":
                return host.text(R.string.recording_status_recording_short);
            case "failed":
            case "error":
                return host.text(R.string.recording_status_issue_short);
            case "scheduled":
                return host.text(R.string.recording_status_scheduled_short);
            default:
                return status.toUpperCase(java.util.Locale.US);
        }
    }

    static int statusBadgeColor(RecordingsRepository.RecordingItem item, RecordingsRepository.RecordingsResult result, PresentationHost host) {
        if (item == null || item.status == null) {
            return 0xFF4F3A23;
        }
        if (host.hasConflict(item, result)) {
            return 0xFF9A6B28;
        }
        String status = item.status.trim().toLowerCase(java.util.Locale.US);
        switch (status) {
            case "completed":
                return 0xFF2E6A57;
            case "recording":
                return 0xFF8B3D2F;
            case "scheduled":
                return 0xFF3F5877;
            case "failed":
            case "error":
                return 0xFF7A3340;
            default:
                return 0xFF4F3A23;
        }
    }

    static int metaColor(RecordingsRepository.RecordingItem item, PresentationHost host) {
        if (item == null || item.playable) {
            return 0xFFF2D5AF;
        }
        switch (host.safeLower(item.status)) {
            case "recording":
            case "running":
            case "in_progress":
                return 0xFF8DE1A5;
            case "failed":
            case "error":
                return 0xFFFF9C9C;
            case "cancelled":
            case "canceled":
                return 0xFFC7D2E2;
            default:
                return 0xFF9BD0FF;
        }
    }
}
