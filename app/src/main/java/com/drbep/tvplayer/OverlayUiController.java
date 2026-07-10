package com.drbep.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import androidx.compose.ui.platform.ComposeView;

/**
 * Controla los "badges" transitorios superpuestos al reproductor (estado, error y
 * distintivo HDR), extraidos de MainActivity como primer paso de la descomposicion
 * del monolito. MainActivity delega en este controlador mediante {@link Host} para
 * las decisiones que dependen del estado global de reproduccion.
 */
final class OverlayUiController {

    interface Host {
        /** Evita repetir en pantalla estados de reproduccion redundantes. */
        boolean isRedundantPlaybackStatus(String text);

        /** Notifica que el panel de overlay debe recalcularse tras cambiar el estado. */
        void onOverlayPanelInvalidated();

        /** Canal en reproduccion actual (para adaptar el mensaje de error VOD/directo). */
        ChannelItem currentPlaybackChannelItem();

        /** Nombre visible del canal para los mensajes de error. */
        String displayName(ChannelItem item);

        /** URL base actual para el detalle del error de directo. */
        String baseUrl();
    }

    private static final long STATUS_HIDE_MS = 2500L;
    private static final long HDR_BADGE_HIDE_MS = 2000L;

    private final Context context;
    private final Handler uiHandler;
    private final Host host;

    private ComposeView statusText;
    private ComposeView errorText;
    private ComposeView hdrBadgeText;
    private ComposeView startupLoadingOverlay;

    private final Runnable hideStatusRunnable = () -> {
        if (statusText != null) {
            statusText.setVisibility(View.GONE);
        }
    };
    private final Runnable hideHdrBadgeRunnable = () -> {
        if (hdrBadgeText != null) {
            hdrBadgeText.setVisibility(View.GONE);
        }
    };

    OverlayUiController(Context context, Handler uiHandler, Host host) {
        this.context = context;
        this.uiHandler = uiHandler;
        this.host = host;
    }

    /** Enlaza las vistas Compose una vez infladas en onCreate. */
    void attachViews(ComposeView statusText, ComposeView errorText, ComposeView hdrBadgeText,
                     ComposeView startupLoadingOverlay) {
        this.statusText = statusText;
        this.errorText = errorText;
        this.hdrBadgeText = hdrBadgeText;
        this.startupLoadingOverlay = startupLoadingOverlay;
    }

    void showStartupLoading(String step, String detail) {
        showLoading(context.getString(R.string.startup_loading_title), step, detail);
    }

    void showLoading(String title, String step, String detail) {
        if (startupLoadingOverlay == null) {
            return;
        }
        StartupLoadingComposeBinder.bind(startupLoadingOverlay,
                new StartupLoadingUiModel(title, step, detail));
        startupLoadingOverlay.setVisibility(View.VISIBLE);
    }

    void updateStartupLoading(String step, String detail) {
        updateLoading(context.getString(R.string.startup_loading_title), step, detail);
    }

    void updateLoading(String title, String step, String detail) {
        if (startupLoadingOverlay == null || startupLoadingOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        StartupLoadingComposeBinder.bind(startupLoadingOverlay,
                new StartupLoadingUiModel(title, step, detail));
    }

    void hideStartupLoading() {
        if (startupLoadingOverlay != null) {
            startupLoadingOverlay.setVisibility(View.GONE);
        }
    }

    void showStatus(String text) {
        if (statusText == null || text == null || text.trim().isEmpty()) {
            return;
        }
        if (host.isRedundantPlaybackStatus(text)) {
            statusText.setVisibility(View.GONE);
            host.onOverlayPanelInvalidated();
            uiHandler.removeCallbacks(hideStatusRunnable);
            return;
        }
        SurfaceBadgeComposeBinder.bind(statusText,
                new SurfaceBadgeUiModel(text, 0xB3000000, 0xFFFFFFFF, true, false));
        statusText.setVisibility(View.VISIBLE);
        host.onOverlayPanelInvalidated();
        uiHandler.removeCallbacks(hideStatusRunnable);
        uiHandler.postDelayed(hideStatusRunnable, STATUS_HIDE_MS);
    }

    void showHdrBadge(String label) {
        if (hdrBadgeText == null) {
            return;
        }
        String text = label == null || label.trim().isEmpty()
                ? context.getString(R.string.status_hdr_detected)
                : label.trim();
        SurfaceBadgeComposeBinder.bind(hdrBadgeText,
                new SurfaceBadgeUiModel(text, 0xE0A86A00, 0xFFFFFFFF, false, false));
        hdrBadgeText.setVisibility(View.VISIBLE);
        uiHandler.removeCallbacks(hideHdrBadgeRunnable);
        uiHandler.postDelayed(hideHdrBadgeRunnable, HDR_BADGE_HIDE_MS);
    }

    void showError(String reason) {
        hideStartupLoading();
        if (errorText == null) {
            return;
        }
        String safeReason = reason == null
                ? context.getString(R.string.error_unknown_reason) : reason;
        ChannelItem current = host.currentPlaybackChannelItem();
        if (current != null && current.isVod) {
            errorText.setVisibility(View.VISIBLE);
            SurfaceBadgeComposeBinder.bind(errorText, new SurfaceBadgeUiModel(context.getString(
                    R.string.error_vod_playback_details,
                    safeReason,
                    host.displayName(current)
            ), 0xCC000000, 0xFFFFFFFF, true, true));
            return;
        }
        errorText.setVisibility(View.VISIBLE);
        SurfaceBadgeComposeBinder.bind(errorText, new SurfaceBadgeUiModel(context.getString(
                R.string.error_playback_details,
                safeReason,
                host.baseUrl()
        ), 0xCC000000, 0xFFFFFFFF, true, true));
    }

    void hideError() {
        if (errorText != null) {
            errorText.setVisibility(View.GONE);
        }
    }
}
