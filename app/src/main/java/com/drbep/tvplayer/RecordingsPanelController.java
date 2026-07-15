package com.drbep.tvplayer;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.compose.ui.platform.ComposeView;

/**
 * Controla el panel interactivo de grabaciones: estado de foco de cabecera,
 * auto-refresco, navegación de selección y renderizado de la superficie Compose.
 *
 * MainActivity delega en este controlador mediante wrappers finos y expone
 * los helpers de formato/acciones (compartidos con diálogos) a través de {@link Host}.
 */
final class RecordingsPanelController {

    private static final String TAG = "DRBEP-TV-Native";
    private static final long AUTO_REFRESH_MS = 60000L;

    interface Host {
        String string(int resId);
        String string(int resId, Object... args);
        String summary(RecordingsRepository.RecordingsResult result);
        String hint();
        String title(RecordingsRepository.RecordingItem item);
        String meta(RecordingsRepository.RecordingItem item);
        int metaColor(RecordingsRepository.RecordingItem item);
        String statusLabel(RecordingsRepository.RecordingItem item);
        int statusBadgeColor(RecordingsRepository.RecordingItem item);
        void bindPoster(ImageView imageView, String posterUrl);
        void switchMode(boolean scheduledMode);
        void refreshData();
        void playRecording(RecordingsRepository.RecordingItem item, String basePath);
        void showRecordingActionsDialog();
        void showRecordingsDialog(RecordingsRepository.RecordingsResult result);
        void onBeforeShowPanel();
    }

    private final Handler uiHandler;
    private final RecordingsController recordingsController;
    private final OfflineComposeSurfaceRenderer composeSurfaceRenderer;
    private final Host host;

    private ComposeView panel;
    private int headerFocusIndex = -1;
    private boolean headerFocusActive;
    private boolean autoRefreshEnabled;
    private int pendingScrollIndex = -1;

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!autoRefreshEnabled || !isVisible()) {
                return;
            }
            host.refreshData();
            uiHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    RecordingsPanelController(Handler uiHandler, RecordingsController recordingsController, OfflineComposeSurfaceRenderer composeSurfaceRenderer, Host host) {
        this.uiHandler = uiHandler;
        this.recordingsController = recordingsController;
        this.composeSurfaceRenderer = composeSurfaceRenderer;
        this.host = host;
    }

    void attachPanel(ComposeView panel) {
        this.panel = panel;
    }

    boolean isVisible() {
        return panel != null && panel.getVisibility() == View.VISIBLE;
    }

    boolean isAutoRefreshEnabled() {
        return autoRefreshEnabled;
    }

    boolean toggleAutoRefresh() {
        autoRefreshEnabled = !autoRefreshEnabled;
        scheduleAutoRefresh();
        return autoRefreshEnabled;
    }

    void show(RecordingsRepository.RecordingsResult result) {
        show(result, null);
    }

    void show(RecordingsRepository.RecordingsResult result, String preferredId) {
        if (panel == null) {
            host.showRecordingsDialog(result);
            return;
        }
        host.onBeforeShowPanel();
        recordingsController.applyResult(result, preferredId);
        headerFocusIndex = result.scheduledMode ? 1 : 0;
        headerFocusActive = false;
        Log.d(TAG, "showRecordingsPanel scheduled=" + result.scheduledMode + " count=" + result.items.size());
        pendingScrollIndex = recordingsController.getSelectedIndex();
        refreshSurface();
        panel.setVisibility(View.VISIBLE);
        scheduleAutoRefresh();
        Log.d(TAG, "recordingsPanel visible=" + (panel.getVisibility() == View.VISIBLE));
    }

    void hide() {
        uiHandler.removeCallbacks(autoRefreshRunnable);
        recordingsController.clearCurrentResult();
        headerFocusIndex = -1;
        headerFocusActive = false;
        if (panel != null) {
            panel.setVisibility(View.GONE);
        }
        refreshSurface();
    }

    void scheduleAutoRefresh() {
        uiHandler.removeCallbacks(autoRefreshRunnable);
        if (autoRefreshEnabled && isVisible()) {
            uiHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
        }
    }

    void moveSelection(int delta) {
        if (recordingsController.moveSelection(delta) == null) {
            return;
        }
        headerFocusActive = false;
        pendingScrollIndex = recordingsController.getSelectedIndex();
        refreshSurface();
    }

    void moveHeaderFocus(int delta) {
        if (!isVisible()) {
            return;
        }
        int anchor = headerFocusIndex;
        if (anchor < 0 || anchor > 2) {
            anchor = recordingsController.isScheduledMode() ? 1 : 0;
        }
        int next = ((anchor + delta) % 3 + 3) % 3;
        headerFocusIndex = next;
        headerFocusActive = true;
        refreshSurface();
    }

    boolean activateHeaderFocus() {
        if (!headerFocusActive || headerFocusIndex < 0) {
            return false;
        }
        switch (headerFocusIndex) {
            case 0:
                host.switchMode(false);
                return true;
            case 1:
                host.switchMode(true);
                return true;
            case 2:
                host.refreshData();
                return true;
            default:
                return false;
        }
    }

    void playSelected() {
        RecordingsRepository.RecordingsResult result = recordingsController.getCurrentResult();
        RecordingsRepository.RecordingItem item = recordingsController.getSelectedItem();
        if (result == null || item == null) {
            return;
        }
        if (!item.playable) {
            host.showRecordingActionsDialog();
            return;
        }
        host.playRecording(item, result.basePath);
    }

    void refreshSurface() {
        if (panel == null) {
            return;
        }
        composeSurfaceRenderer.bindRecordingsSurface(
                panel,
                buildSurfaceUiModel(),
                host::bindPoster
        );
        pendingScrollIndex = -1;
    }

    private RecordingsSurfaceUiModel buildSurfaceUiModel() {
        return RecordingsUiFactory.build(new RecordingsUiFactory.Host() {
            @Override
            public String text(int resId) {
                return host.string(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return host.string(resId, args);
            }

            @Override
            public String summary(RecordingsRepository.RecordingsResult result) {
                return host.summary(result);
            }

            @Override
            public String hint() {
                return host.hint();
            }

            @Override
            public String title(RecordingsRepository.RecordingItem item) {
                return host.title(item);
            }

            @Override
            public String meta(RecordingsRepository.RecordingItem item) {
                return host.meta(item);
            }

            @Override
            public int metaColor(RecordingsRepository.RecordingItem item) {
                return host.metaColor(item);
            }

            @Override
            public String statusLabel(RecordingsRepository.RecordingItem item) {
                return host.statusLabel(item);
            }

            @Override
            public int statusBadgeColor(RecordingsRepository.RecordingItem item) {
                return host.statusBadgeColor(item);
            }

            @Override
            public RecordingsRepository.RecordingsResult currentResult() {
                return recordingsController.getCurrentResult();
            }

            @Override
            public RecordingsRepository.RecordingItem selectedItem() {
                return recordingsController.getSelectedItem();
            }

            @Override
            public int selectedIndex() {
                return recordingsController.getSelectedIndex();
            }

            @Override
            public boolean scheduledMode() {
                return recordingsController.isScheduledMode();
            }

            @Override
            public boolean headerFocusActive() {
                return headerFocusActive;
            }

            @Override
            public int headerFocusIndex() {
                return headerFocusIndex;
            }

            @Override
            public int pendingScrollIndex() {
                return pendingScrollIndex;
            }

            @Override
            public void switchMode(boolean scheduledMode) {
                host.switchMode(scheduledMode);
            }

            @Override
            public void refresh() {
                host.refreshData();
            }

            @Override
            public void selectAndPlay(int position, RecordingsRepository.RecordingItem item, String basePath) {
                recordingsController.selectIndex(position);
                pendingScrollIndex = position;
                refreshSurface();
                if (item != null) {
                    host.playRecording(item, basePath);
                }
            }
        });
    }
}
