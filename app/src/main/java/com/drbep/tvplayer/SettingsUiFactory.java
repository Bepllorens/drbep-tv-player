package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class SettingsUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        boolean startupEnabled();
        String updateChannelLabel();
        int recentSearchCount();
        boolean playbackRepairEnabled();
        void openStartup();
        void openPlayback();
        void openSearch();
        void openRecordings();
        void openLocalData();
        void openParental();
        void openOfflineSystem();
        void runFullSync();
        void openOfflineCatalog();
        void checkUpdate();
        void openUpdateChannel();
        void checkRescueUpdate();
        void openDiagnostics();
        void openReset();
        void toggleStartup();
        void showStartupNow();
        void setStartupChannel();
        void clearLastVod();
        void showStartupSummary();
        void openGlobalSearch();
        void clearRecentSearches();
        void showSearchSummary();
        void togglePlaybackRepair();
        void openCurrentPlaybackMode();
        void openPlaybackDiagnostics();
        void clearLearnedPlaybackModes();
        void clearPlaybackModes();
        void clearPlaybackDiagnostics();
        void showPlaybackSummary();
        boolean offlineRecordingsDisabled();
        boolean recordingsAutoRefreshEnabled();
        boolean parentalPinConfigured();
        boolean parentalUnlocked();
        String recordingsOfflineSummary();
        void toggleRecordingsAutoRefresh();
        void openRecordingsBrowser();
        void clearRecordingProgress();
        void showRecordingsSummary();
        void clearVodProgress();
        void clearRecentChannels();
        void clearFavorites();
        void resetListsAndProfiles();
        void showLocalDataSummary();
        void showParentalStatus();
        void toggleParentalLock();
        void changeParentalPin();
        void clearParentalPin();
        void setParentalPin();
    }

    private SettingsUiFactory() {
    }

    static TvOptionsMenuModel buildCenter(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.settings_section_startup), host::openStartup);
        add(options, actions, host.text(R.string.settings_section_playback), host::openPlayback);
        add(options, actions, host.text(R.string.settings_section_search), host::openSearch);
        add(options, actions, host.text(R.string.settings_section_recordings), host::openRecordings);
        add(options, actions, host.text(R.string.settings_section_local_data), host::openLocalData);
        add(options, actions, host.text(R.string.settings_section_parental), host::openParental);
        add(options, actions, host.text(R.string.settings_section_offline_system), host::openOfflineSystem);
        add(options, actions, host.text(R.string.settings_offline_full_sync), host::runFullSync);
        add(options, actions, host.text(R.string.settings_section_offline_catalog), host::openOfflineCatalog);
        add(options, actions, host.text(R.string.app_update_action_check), host::checkUpdate);
        add(options, actions, host.text(R.string.app_update_channel_action, host.updateChannelLabel()), host::openUpdateChannel);
        add(options, actions, host.text(R.string.app_update_action_rescue), host::checkRescueUpdate);
        add(options, actions, host.text(R.string.settings_section_diagnostics), host::openDiagnostics);
        add(options, actions, host.text(R.string.settings_section_reset), host::openReset);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildStartup(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(host.startupEnabled() ? R.string.settings_startup_disable : R.string.settings_startup_enable), host::toggleStartup);
        add(options, actions, host.text(R.string.settings_startup_show_now), host::showStartupNow);
        add(options, actions, host.text(R.string.settings_startup_set_current_channel), host::setStartupChannel);
        add(options, actions, host.text(R.string.settings_startup_clear_last_vod), host::clearLastVod);
        add(options, actions, host.text(R.string.settings_action_view_summary), host::showStartupSummary);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildSearch(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.quick_hub_global_search), host::openGlobalSearch);
        add(options, actions, host.text(R.string.settings_search_clear_recent), host::clearRecentSearches);
        add(options, actions, host.text(R.string.settings_action_view_summary), host::showSearchSummary);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildPlayback(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(host.playbackRepairEnabled() ? R.string.settings_playback_repair_disable : R.string.settings_playback_repair_enable), host::togglePlaybackRepair);
        add(options, actions, host.text(R.string.settings_playback_current_mode), host::openCurrentPlaybackMode);
        add(options, actions, host.text(R.string.settings_playback_diagnostics), host::openPlaybackDiagnostics);
        add(options, actions, host.text(R.string.settings_playback_clear_learned), host::clearLearnedPlaybackModes);
        add(options, actions, host.text(R.string.settings_playback_clear_modes), host::clearPlaybackModes);
        add(options, actions, host.text(R.string.settings_playback_clear_diagnostics), host::clearPlaybackDiagnostics);
        add(options, actions, host.text(R.string.settings_action_view_summary), host::showPlaybackSummary);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildRecordings(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (!host.offlineRecordingsDisabled()) {
            add(options, actions, host.text(host.recordingsAutoRefreshEnabled() ? R.string.settings_recordings_auto_off : R.string.settings_recordings_auto_on), host::toggleRecordingsAutoRefresh);
            add(options, actions, host.text(R.string.tools_menu_recordings_panel), host::openRecordingsBrowser);
        }
        add(options, actions, host.text(R.string.settings_recordings_clear_progress), host::clearRecordingProgress);
        add(options, actions, host.text(R.string.settings_action_view_summary), host::showRecordingsSummary);
        return new TvOptionsMenuModel(options, actions, host.offlineRecordingsDisabled() ? host.recordingsOfflineSummary() : null);
    }

    static TvOptionsMenuModel buildLocalData(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.settings_data_clear_vod_progress), host::clearVodProgress);
        add(options, actions, host.text(R.string.settings_data_clear_recording_progress), host::clearRecordingProgress);
        add(options, actions, host.text(R.string.settings_data_clear_recent_channels), host::clearRecentChannels);
        add(options, actions, host.text(R.string.settings_data_clear_favorites), host::clearFavorites);
        add(options, actions, host.text(R.string.settings_data_reset_lists_profiles), host::resetListsAndProfiles);
        add(options, actions, host.text(R.string.settings_action_view_summary), host::showLocalDataSummary);
        return new TvOptionsMenuModel(options, actions);
    }

    static TvOptionsMenuModel buildParental(Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        add(options, actions, host.text(R.string.settings_parental_view_status), host::showParentalStatus);
        if (host.parentalPinConfigured()) {
            add(options, actions, host.text(host.parentalUnlocked() ? R.string.settings_parental_lock_now : R.string.settings_parental_unlock), host::toggleParentalLock);
            add(options, actions, host.text(R.string.settings_parental_change_pin), host::changeParentalPin);
            add(options, actions, host.text(R.string.settings_parental_clear_pin), host::clearParentalPin);
        } else {
            add(options, actions, host.text(R.string.settings_parental_set_pin), host::setParentalPin);
        }
        return new TvOptionsMenuModel(options, actions);
    }

    private static void add(List<String> options, List<Runnable> actions, String label, Runnable action) {
        options.add(label);
        actions.add(action);
    }
}
