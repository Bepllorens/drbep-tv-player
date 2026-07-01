package com.drbep.tvplayer;

import java.util.List;

public final class PlaybackDiagnosticsPanelUiModel {
    public final String title;
    public final String subtitle;
    public final String summary;
    public final List<PlaybackDiagnosticsRowUiModel> rows;
    public final List<String> notes;
    public final List<TvMessageActionUiModel> actions;

    public PlaybackDiagnosticsPanelUiModel(String title, String subtitle, String summary, List<PlaybackDiagnosticsRowUiModel> rows, List<String> notes, List<TvMessageActionUiModel> actions) {
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.summary = summary == null ? "" : summary;
        this.rows = rows;
        this.notes = notes;
        this.actions = actions;
    }
}
