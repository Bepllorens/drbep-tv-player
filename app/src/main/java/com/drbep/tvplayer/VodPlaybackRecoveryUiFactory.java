package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class VodPlaybackRecoveryUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String displayName(ChannelItem item);
        void retry(ChannelItem item);
        void retryRoute(ChannelItem item);
        void diagnostics(ChannelItem item);
        void library();
    }

    private VodPlaybackRecoveryUiFactory() {
    }

    static TvMessagePanelUiModel build(ChannelItem channel, String error, Host host) {
        List<TvMessageActionUiModel> actions = new ArrayList<>();
        if (channel != null && host != null) {
            actions.add(new TvMessageActionUiModel(host.text(R.string.vod_recovery_retry), false, () -> host.retry(channel)));
            actions.add(new TvMessageActionUiModel(host.text(R.string.vod_action_retry_route), false, () -> host.retryRoute(channel)));
            actions.add(new TvMessageActionUiModel(host.text(R.string.vod_action_diagnostics), false, () -> host.diagnostics(channel)));
            actions.add(new TvMessageActionUiModel(host.text(R.string.vod_recovery_library), false, host::library));
        }
        actions.add(new TvMessageActionUiModel(host == null ? "" : host.text(R.string.dialog_close), false, null));
        String safeError = error == null || error.trim().isEmpty()
                ? host.text(R.string.error_unknown_reason)
                : error.trim();
        return new TvMessagePanelUiModel(
                host.text(R.string.vod_recovery_title, host.displayName(channel)),
                host.text(R.string.vod_recovery_message, safeError),
                actions
        );
    }
}
