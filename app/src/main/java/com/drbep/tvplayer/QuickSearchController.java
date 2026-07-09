package com.drbep.tvplayer;

import android.os.Handler;
import android.view.View;

import androidx.compose.ui.platform.ComposeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controla el overlay de búsqueda rápida (quick search) durante la reproducción:
 * captura de caracteres, coincidencias de canales, selección, auto-ocultado
 * temporizado y renderizado de la superficie Compose.
 *
 * MainActivity delega mediante wrappers finos y expone la búsqueda de canales y
 * el sintonizado a través de {@link Host}.
 */
final class QuickSearchController {

    private static final long AUTO_CLEAR_MS = 3200L;

    interface Host {
        String string(int resId);
        String string(int resId, Object... args);
        List<ChannelItem> searchChannels(String query, int limit);
        void tuneChannelById(String channelId);
    }

    private final Handler uiHandler;
    private final Host host;

    private ComposeView overlay;
    private final StringBuilder buffer = new StringBuilder();
    private final List<ChannelItem> matches = new ArrayList<>();
    private int selectionIndex = 0;

    private final Runnable clearRunnable = this::clear;

    QuickSearchController(Handler uiHandler, Host host) {
        this.uiHandler = uiHandler;
        this.host = host;
    }

    void attachOverlay(ComposeView overlay) {
        this.overlay = overlay;
    }

    boolean isVisible() {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    void handleCharacter(char value) {
        if (!Character.isLetterOrDigit(value)) {
            return;
        }
        buffer.append(Character.toLowerCase(value));
        update();
    }

    void deleteCharacter() {
        if (buffer.length() == 0) {
            clear();
            return;
        }
        buffer.deleteCharAt(buffer.length() - 1);
        if (buffer.length() == 0) {
            clear();
            return;
        }
        update();
    }

    void moveSelection(int delta) {
        if (matches.isEmpty()) {
            return;
        }
        selectionIndex += delta;
        if (selectionIndex < 0) {
            selectionIndex = matches.size() - 1;
        }
        if (selectionIndex >= matches.size()) {
            selectionIndex = 0;
        }
        update();
    }

    void tuneSelection() {
        if (matches.isEmpty()) {
            return;
        }
        if (selectionIndex < 0 || selectionIndex >= matches.size()) {
            selectionIndex = 0;
        }
        host.tuneChannelById(matches.get(selectionIndex).id);
        clear();
    }

    void clear() {
        buffer.setLength(0);
        matches.clear();
        selectionIndex = 0;
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
        uiHandler.removeCallbacks(clearRunnable);
    }

    private void update() {
        if (overlay == null) {
            return;
        }
        String query = buffer.toString().trim();
        if (query.isEmpty()) {
            clear();
            return;
        }
        matches.clear();
        matches.addAll(host.searchChannels(query, 6));
        if (selectionIndex >= matches.size()) {
            selectionIndex = 0;
        }
        overlay.setVisibility(View.VISIBLE);
        String resultText;
        if (matches.isEmpty()) {
            resultText = host.string(R.string.quick_search_no_results);
        } else {
            ChannelItem selected = matches.get(selectionIndex);
            String primaryMeta = selected.nowProgram != null && !selected.nowProgram.trim().isEmpty()
                    ? selected.nowProgram
                    : selected.group;
            if (primaryMeta == null || primaryMeta.trim().isEmpty()) {
                primaryMeta = host.string(R.string.search_channel_action_hint);
            }
            resultText = host.string(
                    R.string.quick_search_result,
                    host.string(R.string.quick_search_result_index, selectionIndex + 1, matches.size())
                            + "  ·  " + selected.name,
                    primaryMeta
            );
        }
        QuickSearchOverlayComposeBinder.bind(
                overlay,
                new QuickSearchOverlayUiModel(
                        host.string(R.string.quick_search_title),
                        query.toUpperCase(Locale.getDefault()),
                        resultText,
                        host.string(R.string.quick_search_hint)
                )
        );
        uiHandler.removeCallbacks(clearRunnable);
        uiHandler.postDelayed(clearRunnable, AUTO_CLEAR_MS);
    }
}
