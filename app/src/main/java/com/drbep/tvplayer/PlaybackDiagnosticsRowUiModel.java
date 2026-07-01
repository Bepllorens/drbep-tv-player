package com.drbep.tvplayer;

public final class PlaybackDiagnosticsRowUiModel {
    public final String section;
    public final String label;
    public final String value;
    public final String tone;

    public PlaybackDiagnosticsRowUiModel(String section, String label, String value, String tone) {
        this.section = section == null ? "" : section;
        this.label = label == null ? "" : label;
        this.value = value == null ? "" : value;
        this.tone = tone == null ? "" : tone;
    }
}
