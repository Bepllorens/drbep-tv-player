package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class PlaybackDiagnosticsUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String displayName(ChannelItem item);
        String safeText(String value);
        String safeDiagnosticUrl(String value);
        String fallbackUnknown(String value);
        String routeTone(String routeLabel);
        String playbackQuality(PlayerController.PlaybackDiagnostics diagnostics);
        String playbackModeLabel(String playbackMode);
        String classifyError(String error);
        String recommendation(PlayerController.PlaybackDiagnostics diagnostics, PlaybackDiagnosticsStore.ErrorRecord storedError);
        String recentSummary();
        String historyItem(PlaybackDiagnosticsStore.ErrorRecord record);
        boolean hasTemporaryMode(ChannelItem item);
        String temporaryMode(ChannelItem item);
        boolean hasLearnedMode(ChannelItem item);
        String learnedMode(ChannelItem item);
        void retryCurrentPlayback();
        void retryWithNextRoute(ChannelItem item);
        void testMode(ChannelItem item, String mode);
        void showAudioTracks();
        void showTemporaryMode(ChannelItem item);
        void showPermanentMode(ChannelItem item);
        void saveLearned(ChannelItem item);
        void clearLearned(ChannelItem item);
        void showActions(ChannelItem item);
        void showHistory();
        void clearError(ChannelItem item);
        void clearHistory();
    }

    private PlaybackDiagnosticsUiFactory() {
    }

    static PlaybackDiagnosticsPanelUiModel buildCurrent(PlayerController.PlaybackDiagnostics diagnostics, ChannelItem currentChannel, PlaybackDiagnosticsStore.ErrorRecord storedError, Host host) {
        if (diagnostics == null || (isBlank(diagnostics.channelName) && isBlank(diagnostics.targetUrl))) {
            String message = host.text(R.string.diagnostics_none);
            List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
            rows.add(new PlaybackDiagnosticsRowUiModel(host.text(R.string.title_playback_diagnostics), "Estado", message, "warn"));
            if (storedError != null) {
                message = message + "\n\n" + host.text(R.string.diagnostics_persistent_error, storedError.shortLabel());
                rows.add(new PlaybackDiagnosticsRowUiModel(host.text(R.string.title_playback_diagnostics), "Fallo guardado", storedError.shortLabel(), "error"));
            }
            return new PlaybackDiagnosticsPanelUiModel(
                    host.text(R.string.title_playback_diagnostics),
                    currentChannel == null ? "" : host.displayName(currentChannel),
                    message,
                    rows,
                    Collections.emptyList(),
                    buildPrimaryActions(currentChannel, host)
            );
        }

        StringBuilder message = new StringBuilder();
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        appendLine(message, host.text(R.string.diagnostics_channel, host.safeText(diagnostics.channelName)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Reproduccion", "Canal", host.safeText(diagnostics.channelName), ""));
        appendLine(message, host.text(R.string.diagnostics_state, host.safeText(diagnostics.playbackState)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Reproduccion", "Estado", host.safeText(diagnostics.playbackState), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Reproduccion", "Fase", host.fallbackUnknown(diagnostics.playbackPhase), phaseTone(diagnostics.playbackPhase)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Reproduccion", "Intento", String.valueOf(diagnostics.attemptGeneration), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Tiempos", "Prepare", diagnostics.prepareElapsedMs + " ms", timingTone(diagnostics.prepareElapsedMs, 8_000L)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Tiempos", "Ready", diagnostics.readyElapsedMs + " ms", timingTone(diagnostics.readyElapsedMs, 5_000L)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Tiempos", "Buffering", diagnostics.bufferingCount + " / " + diagnostics.bufferingTotalMs + " ms", diagnostics.bufferingCount > 1 ? "warn" : ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Tiempos", "Primer frame", diagnostics.firstFrameRendered ? "Si" : "No", diagnostics.firstFrameRendered ? "ok" : "warn"));
        appendLine(message, host.text(R.string.diagnostics_route, host.safeText(diagnostics.routeLabel)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ruta", "Ruta activa", host.safeText(diagnostics.routeLabel), host.routeTone(diagnostics.routeLabel)));
        String safeTargetUrl = host.safeDiagnosticUrl(diagnostics.targetUrl);
        appendLine(message, host.text(R.string.diagnostics_target, safeTargetUrl));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ruta", "URL efectiva", safeTargetUrl, ""));
        appendLine(message, host.text(R.string.diagnostics_mime, host.fallbackUnknown(diagnostics.mimeType)));
        rows.add(new PlaybackDiagnosticsRowUiModel("Ruta", "Mime", host.fallbackUnknown(diagnostics.mimeType), ""));
        if (diagnostics.hasVideoQuality()) {
            String quality = host.playbackQuality(diagnostics);
            appendLine(message, host.text(R.string.diagnostics_video_quality, quality));
            rows.add(new PlaybackDiagnosticsRowUiModel("Calidad", "Video", quality, "ok"));
        }
        appendLine(message, host.text(R.string.diagnostics_drm, host.fallbackUnknown(diagnostics.drmType)));
        rows.add(new PlaybackDiagnosticsRowUiModel("DRM", "DRM", host.fallbackUnknown(diagnostics.drmType), diagnostics.encrypted ? "warn" : ""));
        appendLine(message, host.text(R.string.diagnostics_playback_mode, host.playbackModeLabel(diagnostics.playbackMode)));
        rows.add(new PlaybackDiagnosticsRowUiModel("DRM", "Modo canal", host.playbackModeLabel(diagnostics.playbackMode), ""));
        appendLine(message, host.text(R.string.diagnostics_encrypted, host.text(diagnostics.encrypted ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        rows.add(new PlaybackDiagnosticsRowUiModel("DRM", "Encrypted", host.text(diagnostics.encrypted ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no), diagnostics.encrypted ? "warn" : "ok"));
        appendLine(message, host.text(R.string.diagnostics_fallback, host.text(diagnostics.usingFallback ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no)));
        rows.add(new PlaybackDiagnosticsRowUiModel("DRM", "Compat fallback", host.text(diagnostics.usingFallback ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no), diagnostics.usingFallback ? "warn" : ""));

        String diagnosticErrorText = "";
        if (!isBlank(diagnostics.lastError)) {
            diagnosticErrorText = diagnostics.lastError;
            appendLine(message, host.text(R.string.diagnostics_last_error, diagnostics.lastError));
            rows.add(new PlaybackDiagnosticsRowUiModel("Error", "Ultimo error", diagnostics.lastError, "error"));
        }
        if (storedError != null) {
            if (isBlank(diagnosticErrorText)) {
                diagnosticErrorText = storedError.message;
            }
            appendLine(message, host.text(R.string.diagnostics_persistent_error, storedError.shortLabel()));
            rows.add(new PlaybackDiagnosticsRowUiModel("Error", "Fallo guardado", storedError.shortLabel(), "error"));
            if (!storedError.routeLabel.isEmpty()) {
                appendLine(message, host.text(R.string.diagnostics_persistent_route, storedError.routeLabel));
                rows.add(new PlaybackDiagnosticsRowUiModel("Error", "Ruta del fallo", storedError.routeLabel, "warn"));
            }
        }
        if (!isBlank(diagnosticErrorText)) {
            String errorType = host.classifyError(diagnosticErrorText);
            appendLine(message, host.text(R.string.diagnostics_error_type, errorType));
            rows.add(new PlaybackDiagnosticsRowUiModel("Error", "Tipo", errorType, "warn"));
        }

        String recommendation = host.recommendation(diagnostics, storedError);
        appendLine(message, host.text(R.string.diagnostics_recommendation, recommendation));
        if (currentChannel != null && host.hasTemporaryMode(currentChannel)) {
            String temporaryMode = host.playbackModeLabel(host.temporaryMode(currentChannel));
            appendLine(message, host.text(R.string.diagnostics_temporary_mode, temporaryMode));
            rows.add(new PlaybackDiagnosticsRowUiModel("Preferencias", "Modo temporal", temporaryMode, "warn"));
        }
        if (currentChannel != null && host.hasLearnedMode(currentChannel)) {
            String learnedMode = host.playbackModeLabel(host.learnedMode(currentChannel));
            appendLine(message, host.text(R.string.diagnostics_learned_mode, learnedMode));
            rows.add(new PlaybackDiagnosticsRowUiModel("Preferencias", "Ruta aprendida", learnedMode, "ok"));
        }
        appendLine(message, host.text(R.string.diagnostics_recent, host.recentSummary()));
        appendLine(message, host.text(R.string.diagnostics_actions_hint));

        List<String> notes = new ArrayList<>();
        notes.add(host.text(R.string.diagnostics_recommendation, recommendation));
        notes.add(host.text(R.string.diagnostics_recent, host.recentSummary()));
        notes.add(host.text(R.string.diagnostics_actions_hint));
        return new PlaybackDiagnosticsPanelUiModel(
                host.text(R.string.title_playback_diagnostics),
                host.safeText(diagnostics.channelName),
                message.toString().trim(),
                rows,
                notes,
                buildPrimaryActions(currentChannel, host)
        );
    }

    static TvOptionsMenuModel buildActionsMenu(ChannelItem channelItem, Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (channelItem == null) {
            return new TvOptionsMenuModel(options, actions, "");
        }
        options.add(host.text(R.string.diagnostics_action_retry_next_route));
        actions.add(() -> host.retryWithNextRoute(channelItem));
        options.add(host.text(R.string.diagnostics_action_retry));
        actions.add(host::retryCurrentPlayback);
        options.add(host.text(R.string.diagnostics_action_test_auto));
        actions.add(() -> host.testMode(channelItem, PlaybackModeStore.MODE_AUTO));
        options.add(host.text(R.string.diagnostics_action_test_direct));
        actions.add(() -> host.testMode(channelItem, PlaybackModeStore.MODE_DIRECT));
        options.add(host.text(R.string.diagnostics_action_test_proxy));
        actions.add(() -> host.testMode(channelItem, PlaybackModeStore.MODE_PROXY));
        options.add(host.text(R.string.audio_track_action));
        actions.add(host::showAudioTracks);
        options.add(host.text(R.string.diagnostics_action_temporary_mode));
        actions.add(() -> host.showTemporaryMode(channelItem));
        options.add(host.text(R.string.diagnostics_action_permanent_mode));
        actions.add(() -> host.showPermanentMode(channelItem));
        options.add(host.text(R.string.diagnostics_action_save_learned));
        actions.add(() -> host.saveLearned(channelItem));
        options.add(host.text(R.string.diagnostics_action_clear_learned));
        actions.add(() -> host.clearLearned(channelItem));
        options.add(host.text(R.string.diagnostics_action_history));
        actions.add(host::showHistory);
        options.add(host.text(R.string.diagnostics_action_clear_error));
        actions.add(() -> host.clearError(channelItem));
        return new TvOptionsMenuModel(options, actions, host.displayName(channelItem));
    }

    static PlaybackDiagnosticsPanelUiModel buildHistory(List<PlaybackDiagnosticsStore.ErrorRecord> records, Host host) {
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        int index = 1;
        for (PlaybackDiagnosticsStore.ErrorRecord record : records) {
            String section = record == null ? host.text(R.string.diagnostics_action_history) : record.shortLabel();
            rows.add(new PlaybackDiagnosticsRowUiModel(section, "Canal", record == null ? "" : host.fallbackUnknown(record.channelName), ""));
            rows.add(new PlaybackDiagnosticsRowUiModel(section, "Ruta", record == null ? "" : host.fallbackUnknown(record.routeLabel), record == null ? "" : host.routeTone(record.routeLabel)));
            rows.add(new PlaybackDiagnosticsRowUiModel(section, "Modo", record == null ? "" : host.playbackModeLabel(record.playbackMode), ""));
            rows.add(new PlaybackDiagnosticsRowUiModel(section, "Error", record == null ? "" : host.fallbackUnknown(record.message), "error"));
            if (index <= 3 && record != null) {
                notes.add(index + ". " + host.historyItem(record));
            }
            index++;
        }
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(host.text(R.string.diagnostics_action_clear_history), true, host::clearHistory));
        actions.add(new TvMessageActionUiModel(host.text(R.string.dialog_close), false, null));
        return new PlaybackDiagnosticsPanelUiModel(
                host.text(R.string.diagnostics_action_history),
                host.text(R.string.title_playback_diagnostics),
                host.text(R.string.diagnostics_recent, records.size() + " fallos guardados"),
                rows,
                notes,
                actions
        );
    }

    private static List<TvMessageActionUiModel> buildPrimaryActions(ChannelItem currentChannel, Host host) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        actions.add(new TvMessageActionUiModel(host.text(currentChannel == null ? R.string.diagnostics_action_retry : R.string.diagnostics_action_retry_next_route), false, () -> {
            if (currentChannel == null) {
                host.retryCurrentPlayback();
            } else {
                host.retryWithNextRoute(currentChannel);
            }
        }));
        if (currentChannel != null) {
            actions.add(new TvMessageActionUiModel(host.text(R.string.diagnostics_action_more), false, () -> host.showActions(currentChannel)));
        }
        actions.add(new TvMessageActionUiModel(host.text(R.string.dialog_close), false, null));
        return actions;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (builder == null || line == null || line.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String phaseTone(String phase) {
        String normalized = phase == null ? "" : phase.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("playing")) {
            return "ok";
        }
        if (normalized.contains("error")) {
            return "error";
        }
        if (normalized.contains("buffer") || normalized.contains("waiting")) {
            return "warn";
        }
        return "";
    }

    private static String timingTone(long elapsedMs, long warnAfterMs) {
        return elapsedMs > warnAfterMs ? "warn" : "";
    }
}
