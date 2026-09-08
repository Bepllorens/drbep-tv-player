package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class AppUpdateUiFactoryTest {
    @Test
    public void requiredUpdateOnlyOffersInstall() {
        PlaybackDiagnosticsPanelUiModel model = AppUpdateUiFactory.buildAvailable(update(true), new FakeHost());

        assertEquals(1, model.actions.size());
        assertEquals("Instalar", model.actions.get(0).label);
    }

    @Test
    public void optionalUpdateCanBeDeferred() {
        PlaybackDiagnosticsPanelUiModel model = AppUpdateUiFactory.buildAvailable(update(false), new FakeHost());

        assertEquals(2, model.actions.size());
        assertEquals("Instalar", model.actions.get(0).label);
        assertEquals("Mas tarde", model.actions.get(1).label);
    }

    private static AppUpdateManager.UpdateInfo update(boolean required) {
        return new AppUpdateManager.UpdateInfo(
                true,
                BuildConfig.VERSION_CODE + 1,
                "next",
                "stable",
                "https://example.com/app.apk",
                "abc",
                required,
                Collections.singletonList("Cambio")
        );
    }

    private static final class FakeHost implements AppUpdateUiFactory.Host {
        @Override public String text(int resId) {
            if (resId == R.string.app_update_action_install) return "Instalar";
            if (resId == R.string.app_update_action_later) return "Mas tarde";
            if (resId == R.string.diagnostics_value_yes) return "Si";
            if (resId == R.string.diagnostics_value_no) return "No";
            if (resId == R.string.diagnostics_value_unknown) return "Desconocido";
            return "res:" + resId;
        }

        @Override public String text(int resId, Object... args) {
            return "res:" + resId;
        }

        @Override public String currentVersion() {
            return "actual";
        }

        @Override public String currentChannelLabel() {
            return "stable";
        }

        @Override public String safeVersionName(AppUpdateManager.UpdateInfo info) {
            return info == null ? "" : info.versionName;
        }

        @Override public void install(AppUpdateManager.UpdateInfo info) {
        }
    }
}
