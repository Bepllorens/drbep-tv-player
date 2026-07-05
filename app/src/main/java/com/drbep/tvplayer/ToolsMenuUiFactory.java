package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class ToolsMenuUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        boolean recordingsAvailable();
        boolean canScheduleRecordings();
        String updateChannelLabel();
        void openTvGuide();
        void openRecordings();
        void openVod();
        void refreshCatalog();
        void openSearchRecents();
        void openLists();
        void openFamily();
        void openAdvanced();
        void openCurrentChannel();
        void openPlayback();
        void openNavigation();
        void openMultiview();
        void openSettings();
        void openTimeline();
        void openVisualEpg();
        void openEpgSearch();
        void openChannelSearch();
        void openGlobalSearch();
        void openRecentChannels();
        void openRecentQuick();
        void openFavoritesQuick();
        void openParental();
        void openPersonalListsManager();
        void openParentalStatus();
        void openRecordingsBrowser();
        void recordCurrentProgram();
        void recordNextProgram();
        void retryNextRoute();
        void retryPlayback();
        void openTemporaryPlaybackMode();
        void openAudioTracks();
        void openPlaybackDiagnostics();
        void openQuickHub();
        void openCurrentChannelPersonalLists();
        void openCurrentChannelProfile();
        void openMultiView();
        void openMultiViewPreset();
        void saveMultiViewPreset();
        void openSettingsCenter();
        void openSettingsDiagnostics();
        void openInstallStatus();
        void openUpdateChannel();
    }

    private ToolsMenuUiFactory() {
    }

    static TvOptionsMenuModel buildSimple(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_section_tv_guide), host::openTvGuide);
        if (host.recordingsAvailable()) {
            add(options, actions, host.text(R.string.tools_section_recordings), host::openRecordings);
        }
        add(options, actions, host.text(R.string.tools_section_vod), host::openVod);
        add(options, actions, host.text(R.string.offline_catalog_action_refresh), host::refreshCatalog);
        add(options, actions, host.text(R.string.tools_section_search_recents), host::openSearchRecents);
        add(options, actions, host.text(R.string.tools_section_lists), host::openLists);
        add(options, actions, host.text(R.string.tools_section_family), host::openFamily);
        add(options, actions, host.text(R.string.tools_section_advanced), host::openAdvanced);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildAdvanced(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_section_current_channel), host::openCurrentChannel);
        add(options, actions, host.text(R.string.tools_section_playback), host::openPlayback);
        add(options, actions, host.text(R.string.tools_section_navigation), host::openNavigation);
        add(options, actions, host.text(R.string.tools_section_multiview), host::openMultiview);
        add(options, actions, host.text(R.string.tools_section_settings), host::openSettings);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildTvGuide(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_timeline_guide), host::openTimeline);
        add(options, actions, host.text(R.string.tools_menu_visual_epg), host::openVisualEpg);
        add(options, actions, host.text(R.string.tools_menu_epg_search), host::openEpgSearch);
        add(options, actions, host.text(R.string.tools_menu_search_channels), host::openChannelSearch);
        add(options, actions, host.text(R.string.tools_section_current_channel), host::openCurrentChannel);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildSearchRecents(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.quick_hub_global_search), host::openGlobalSearch);
        add(options, actions, host.text(R.string.tools_menu_search_channels), host::openChannelSearch);
        add(options, actions, host.text(R.string.tools_menu_recent_channels), host::openRecentChannels);
        add(options, actions, host.text(R.string.quick_hub_recent), host::openRecentQuick);
        add(options, actions, host.text(R.string.quick_hub_favorites), host::openFavoritesQuick);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildFamily(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_open_parental), host::openParental);
        add(options, actions, host.text(R.string.tools_menu_favorite_channels), host::openFavoritesQuick);
        add(options, actions, host.text(R.string.tools_menu_manage_personal_lists), host::openPersonalListsManager);
        add(options, actions, host.text(R.string.settings_parental_view_status), host::openParentalStatus);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildRecordings(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_recordings_panel), host::openRecordingsBrowser);
        if (host.canScheduleRecordings()) {
            add(options, actions, host.text(R.string.menu_record_current_program), host::recordCurrentProgram);
            add(options, actions, host.text(R.string.menu_record_next_program), host::recordNextProgram);
        }
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildPlayback(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.diagnostics_action_retry_next_route), host::retryNextRoute);
        add(options, actions, host.text(R.string.diagnostics_action_retry), host::retryPlayback);
        add(options, actions, host.text(R.string.tools_menu_playback_mode_temporary), host::openTemporaryPlaybackMode);
        add(options, actions, host.text(R.string.audio_track_action), host::openAudioTracks);
        add(options, actions, host.text(R.string.tools_menu_playback_diagnostics), host::openPlaybackDiagnostics);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildNavigation(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_quick_hub), host::openQuickHub);
        add(options, actions, host.text(R.string.tools_menu_search_channels), host::openChannelSearch);
        add(options, actions, host.text(R.string.quick_hub_global_search), host::openGlobalSearch);
        add(options, actions, host.text(R.string.tools_menu_timeline_guide), host::openTimeline);
        add(options, actions, host.text(R.string.tools_menu_visual_epg), host::openVisualEpg);
        add(options, actions, host.text(R.string.tools_menu_epg_search), host::openEpgSearch);
        add(options, actions, host.text(R.string.tools_menu_recent_channels), host::openRecentChannels);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildLists(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_favorite_channels), host::openFavoritesQuick);
        add(options, actions, host.text(R.string.tools_menu_manage_personal_lists), host::openPersonalListsManager);
        add(options, actions, host.text(R.string.tools_menu_personal_lists_current), host::openCurrentChannelPersonalLists);
        add(options, actions, host.text(R.string.tools_menu_channel_profile_current), host::openCurrentChannelProfile);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildMultiview(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_multiview), host::openMultiView);
        add(options, actions, host.text(R.string.tools_menu_multiview_open_preset), host::openMultiViewPreset);
        add(options, actions, host.text(R.string.tools_menu_multiview_save_preset), host::saveMultiViewPreset);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildSettings(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.tools_menu_settings_center), host::openSettingsCenter);
        add(options, actions, host.text(R.string.settings_section_diagnostics), host::openSettingsDiagnostics);
        add(options, actions, host.text(R.string.tools_menu_install_status), host::openInstallStatus);
        add(options, actions, host.text(R.string.app_update_channel_action, host.updateChannelLabel()), host::openUpdateChannel);
        return new TvOptionsMenuModel(options, actions);
    }

    private static void add(List<String> options, List<Runnable> actions, String label, Runnable action) {
        options.add(label);
        actions.add(action);
    }
}
