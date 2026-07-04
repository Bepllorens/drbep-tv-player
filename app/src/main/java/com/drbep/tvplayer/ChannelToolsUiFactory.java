package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class ChannelToolsUiFactory {
    interface Host {
        String text(int resId);
        boolean favorite(ChannelItem channelItem);
        void retryRoute(ChannelItem channelItem);
        void temporaryMode(ChannelItem channelItem);
        void audioTracks();
        void toggleFavorite(ChannelItem channelItem);
        void personalLists(ChannelItem channelItem);
        void profile(ChannelItem channelItem);
        void miniGuide(ChannelItem channelItem);
        void diagnostics();
    }

    private ChannelToolsUiFactory() {
    }

    static TvOptionsMenuModel buildCurrentChannel(ChannelItem channelItem, Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (channelItem == null || host == null) {
            return new TvOptionsMenuModel(options, actions);
        }
        options.add(host.text(R.string.diagnostics_action_retry_next_route));
        actions.add(() -> host.retryRoute(channelItem));
        options.add(host.text(R.string.diagnostics_action_temporary_mode));
        actions.add(() -> host.temporaryMode(channelItem));
        options.add(host.text(R.string.audio_track_action));
        actions.add(host::audioTracks);
        options.add(host.text(host.favorite(channelItem) ? R.string.menu_remove_favorite : R.string.menu_add_favorite));
        actions.add(() -> host.toggleFavorite(channelItem));
        options.add(host.text(R.string.menu_personal_lists));
        actions.add(() -> host.personalLists(channelItem));
        options.add(host.text(R.string.menu_channel_profile));
        actions.add(() -> host.profile(channelItem));
        options.add(host.text(R.string.menu_mini_guide));
        actions.add(() -> host.miniGuide(channelItem));
        options.add(host.text(R.string.tools_menu_playback_diagnostics));
        actions.add(host::diagnostics);
        return new TvOptionsMenuModel(options, actions);
    }
}
