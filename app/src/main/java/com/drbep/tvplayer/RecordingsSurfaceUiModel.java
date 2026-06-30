package com.drbep.tvplayer;

public final class RecordingsSurfaceUiModel {
    public final RecordingsPanelUiModel panel;
    public final RecordingListUiModel list;

    public RecordingsSurfaceUiModel(RecordingsPanelUiModel panel, RecordingListUiModel list) {
        this.panel = panel;
        this.list = list;
    }
}
