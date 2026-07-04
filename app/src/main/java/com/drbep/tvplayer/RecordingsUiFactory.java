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
}
