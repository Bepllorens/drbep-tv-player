package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class AppUpdateUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String currentVersion();
        String currentChannelLabel();
        String safeVersionName(AppUpdateManager.UpdateInfo info);
        void install(AppUpdateManager.UpdateInfo info);
    }

    private AppUpdateUiFactory() {
    }

    static PlaybackDiagnosticsPanelUiModel buildAvailable(AppUpdateManager.UpdateInfo info, Host host) {
        List<PlaybackDiagnosticsRowUiModel> rows = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        String safeVersion = host.safeVersionName(info);
        int versionCode = info == null ? 0 : info.versionCode;
        rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Actual", host.currentVersion(), ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Disponible", safeVersion + " (" + versionCode + ")", "ok"));
        rows.add(new PlaybackDiagnosticsRowUiModel("Version", "Canal", info == null || info.channel == null || info.channel.trim().isEmpty() ? host.currentChannelLabel() : info.channel, ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Instalacion", "Obligatoria", host.text(info != null && info.required ? R.string.diagnostics_value_yes : R.string.diagnostics_value_no), info != null && info.required ? "warn" : ""));
        rows.add(new PlaybackDiagnosticsRowUiModel("Instalacion", "APK", info == null || info.apkUrl == null || info.apkUrl.trim().isEmpty() ? host.text(R.string.diagnostics_value_unknown) : info.apkUrl, ""));
        if (info != null && info.sha256 != null && !info.sha256.trim().isEmpty()) {
            rows.add(new PlaybackDiagnosticsRowUiModel("Instalacion", "SHA-256", info.sha256, ""));
        }
        if (info == null || info.changelog == null || info.changelog.isEmpty()) {
            notes.add(host.text(R.string.diagnostics_value_unknown));
        } else {
            for (String item : info.changelog) {
                if (item != null && !item.trim().isEmpty()) {
                    notes.add("- " + item.trim());
                }
            }
        }
        actions.add(new TvMessageActionUiModel(host.text(R.string.app_update_action_install), false, () -> host.install(info)));
        if (info == null || !info.required) {
            actions.add(new TvMessageActionUiModel(host.text(R.string.app_update_action_later), false, null));
        }
        return new PlaybackDiagnosticsPanelUiModel(
                host.text(R.string.app_update_available_title, safeVersion),
                host.text(R.string.app_update_channel_current, host.currentChannelLabel()),
                host.text(R.string.settings_update_state_available_short, safeVersion, versionCode),
                rows,
                notes,
                actions
        );
    }
}
