package com.drbep.tvplayer;

import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.compose.ui.platform.ComposeView;

import java.util.ArrayList;
import java.util.List;

/**
 * Controla el banner de zapping (HUD de acciones rápidas sobre la reproducción):
 * visibilidad, auto-ocultado temporizado, estado de selección de acciones y
 * renderizado de la superficie Compose.
 *
 * MainActivity delega mediante wrappers finos y expone los helpers de formato,
 * las acciones del HUD y el acceso a EPG/diagnósticos a través de {@link Host}.
 */
final class ZapBannerController {

    private static final long AUTO_HIDE_MS = 5500L;

    interface Host {
        String string(int resId);
        String string(int resId, Object... args);
        boolean offlineRecordingsDisabled();
        boolean isProtectedItem(ChannelItem item);
        boolean isFavorite(ChannelItem item);
        ChannelItem currentPlaybackChannel();
        EpgRepository.EpgProgramPair epgPair(String channelId);
        PlayerController.PlaybackDiagnostics playbackDiagnostics();
        void bindChannelLogo(ImageView imageView, String logoUrl, String channelName, int widthDp, int heightDp);
        String displayName(ChannelItem item);
        String channelBadge(ChannelItem item);
        String programMeta(ChannelItem item, EpgRepository.EpgProgram program);
        String playbackQuality(PlayerController.PlaybackDiagnostics diagnostics);
        String durationShort(long durationMs);
        String shortTime(String isoTime);
        long parseIsoMillis(String isoTime);
        // Acciones del HUD.
        void showChannels();
        void openGuide();
        boolean supportsU7d(ChannelItem item);
        void openU7d(ChannelItem item);
        void scheduleCurrentProgram();
        void showParentalSettings();
        void showAudioTrack();
        void showPlaybackDiagnostics();
        void toggleCurrentFavorite();
        void showToolsMenu();
    }

    private final Handler uiHandler;
    private final Host host;

    private ComposeView banner;
    private final ZapBannerState state = new ZapBannerState();
    private final List<ZapActionItem> actionItems = new ArrayList<>();

    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            if (banner != null) {
                banner.setVisibility(View.GONE);
            }
        }
    };

    ZapBannerController(Handler uiHandler, Host host) {
        this.uiHandler = uiHandler;
        this.host = host;
    }

    void attachBanner(ComposeView banner) {
        this.banner = banner;
    }

    boolean isVisible() {
        return state.isVisible() && banner != null && banner.getVisibility() == View.VISIBLE;
    }

    void show(ChannelItem channelItem) {
        if (banner == null || channelItem == null) {
            return;
        }
        state.show();
        updateContent(channelItem);
        banner.setVisibility(View.VISIBLE);
        scheduleAutoHide();
    }

    void updateContent(ChannelItem channelItem) {
        if (banner == null || channelItem == null) {
            return;
        }
        ZapBannerComposeBinder.bind(banner, buildUiModel(channelItem), host::bindChannelLogo);
    }

    void hide() {
        uiHandler.removeCallbacks(hideRunnable);
        if (banner != null) {
            banner.setVisibility(View.GONE);
        }
        state.hide();
    }

    void moveSelection(int delta) {
        if (state.moveSelection(delta, actionItems)) {
            ChannelItem currentChannel = host.currentPlaybackChannel();
            if (currentChannel != null) {
                updateContent(currentChannel);
            } else {
                updateActionButtons(null);
            }
            scheduleAutoHide();
        }
    }

    void activateSelection() {
        ZapActionItem item = state.getSelectedAction(actionItems);
        if (item != null && item.enabled && item.onClick != null) {
            item.onClick.run();
        }
    }

    void refreshAutoHideTimer() {
        scheduleAutoHide();
    }

    private void scheduleAutoHide() {
        uiHandler.removeCallbacks(hideRunnable);
        uiHandler.postDelayed(hideRunnable, AUTO_HIDE_MS);
    }

    private void updateActionButtons(ChannelItem channelItem) {
        int selectedIndex = state.getSelectedActionIndex();
        boolean favorite = channelItem != null && host.isFavorite(channelItem);
        actionItems.clear();
        addActionItem(R.string.zap_action_channels, true, false, selectedIndex, host::showChannels);
        addActionItem(R.string.zap_action_guide, true, false, selectedIndex, host::openGuide);
        if (host.supportsU7d(channelItem)) {
            addActionItem(R.string.zap_action_u7d, true, false, selectedIndex, () -> host.openU7d(channelItem));
        }
        addActionItem(R.string.zap_action_record, !host.offlineRecordingsDisabled(), false, selectedIndex, host::scheduleCurrentProgram);
        addActionItem(R.string.zap_action_family, true, host.isProtectedItem(channelItem), selectedIndex, host::showParentalSettings);
        addActionItem(R.string.zap_action_audio, true, false, selectedIndex, host::showAudioTrack);
        addActionItem(R.string.zap_action_quality, true, false, selectedIndex, host::showPlaybackDiagnostics);
        addActionItem(R.string.zap_action_favorite, true, favorite, selectedIndex, host::toggleCurrentFavorite);
        addActionItem(R.string.zap_action_more, true, false, selectedIndex, host::showToolsMenu);
        state.ensureValidSelection(actionItems);
        int normalizedIndex = state.getSelectedActionIndex();
        for (int i = 0; i < actionItems.size(); i++) {
            ZapActionItem item = actionItems.get(i);
            if (item == null) {
                continue;
            }
            actionItems.set(i, new ZapActionItem(
                    item.label,
                    item.enabled,
                    item.highlighted,
                    i == normalizedIndex,
                    item.onClick,
                    item.onLongClick
            ));
        }
    }

    private void addActionItem(int labelRes, boolean enabled, boolean highlighted, int selectedIndex, Runnable action) {
        int index = actionItems.size();
        actionItems.add(buildActionItem(labelRes, enabled, highlighted, selectedIndex == index, action));
    }

    private ZapActionItem buildActionItem(int labelRes, boolean enabled, boolean highlighted, boolean selected, Runnable action) {
        return new ZapActionItem(
                host.string(labelRes),
                enabled,
                highlighted,
                selected,
                () -> {
                    uiHandler.removeCallbacks(hideRunnable);
                    if (action != null) {
                        action.run();
                    }
                }
        );
    }

    private ZapBannerUiModel buildUiModel(ChannelItem channelItem) {
        updateActionButtons(channelItem);
        EpgRepository.EpgProgramPair pair = host.epgPair(channelItem.id);
        PlayerController.PlaybackDiagnostics diagnostics = host.playbackDiagnostics();
        return ZapBannerUiFactory.build(channelItem, pair, diagnostics, actionItems, new ZapBannerUiFactory.Host() {
            @Override
            public String text(int resId) {
                return host.string(resId);
            }

            @Override
            public String text(int resId, Object... args) {
                return host.string(resId, args);
            }

            @Override
            public String displayName(ChannelItem item) {
                return host.displayName(item);
            }

            @Override
            public String channelBadge(ChannelItem item) {
                return host.channelBadge(item);
            }

            @Override
            public String programMeta(ChannelItem item, EpgRepository.EpgProgram program) {
                return host.programMeta(item, program);
            }

            @Override
            public String playbackQuality(PlayerController.PlaybackDiagnostics playbackDiagnostics) {
                return host.playbackQuality(playbackDiagnostics);
            }

            @Override
            public String durationShort(long durationMs) {
                return host.durationShort(durationMs);
            }

            @Override
            public String shortTime(String isoTime) {
                return host.shortTime(isoTime);
            }

            @Override
            public long parseIsoMillis(String isoTime) {
                return host.parseIsoMillis(isoTime);
            }

            @Override
            public long nowMs() {
                return System.currentTimeMillis();
            }
        });
    }
}
