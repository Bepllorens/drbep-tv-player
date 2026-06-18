package com.drbep.tvplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

final class ChannelActionsCoordinator {
    private static final String TAG = "DRBEP-TV-Native";

    interface Host {
        void tuneSelectedChannel();

        void tuneChannel(ChannelItem channelItem);

        void toggleFavoriteSelected();

        void moveFavoriteSelected(int delta);

        void openPlaybackModeSelector(ChannelItem channelItem);

        void openPersonalListsSelector(ChannelItem channelItem);

        void openChannelProfile(ChannelItem channelItem);

        void openMiniGuide(ChannelItem channelItem);

        void scheduleCurrentProgram(ChannelItem channelItem);

        void scheduleNextProgram(ChannelItem channelItem);

        void createCurrentReminder(ChannelItem channelItem);

        void createNextReminder(ChannelItem channelItem);

        void openRecordings();

        void scheduleProgram(ChannelItem channelItem, EpgRepository.EpgProgram program);

        boolean isProgramScheduled(ChannelItem channelItem, EpgRepository.EpgProgram program);

        void cancelScheduledProgram(ChannelItem channelItem, EpgRepository.EpgProgram program);

        void createReminder(ChannelItem channelItem, EpgRepository.EpgProgram program);
    }

    private final Context context;
    private final Host host;

    ChannelActionsCoordinator(Context context, Host host) {
        this.context = context;
        this.host = host;
    }

    void showChannelActionMenu(ChannelItem channelItem, boolean favorite) {
        if (channelItem == null) {
            return;
        }

        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(context.getString(R.string.menu_tune_channel));
        actions.add(host::tuneSelectedChannel);
        options.add(context.getString(favorite ? R.string.menu_remove_favorite : R.string.menu_add_favorite));
        actions.add(host::toggleFavoriteSelected);
        if (favorite) {
            options.add(context.getString(R.string.menu_favorite_move_up));
            actions.add(() -> host.moveFavoriteSelected(-1));
            options.add(context.getString(R.string.menu_favorite_move_down));
            actions.add(() -> host.moveFavoriteSelected(1));
        }
        options.add(context.getString(R.string.menu_personal_lists));
        actions.add(() -> host.openPersonalListsSelector(channelItem));
        options.add(context.getString(R.string.menu_channel_profile));
        actions.add(() -> host.openChannelProfile(channelItem));
        options.add(context.getString(R.string.menu_playback_mode));
        actions.add(() -> host.openPlaybackModeSelector(channelItem));
        options.add(context.getString(R.string.menu_mini_guide));
        actions.add(() -> host.openMiniGuide(channelItem));
        options.add(context.getString(R.string.menu_record_current_program));
        actions.add(() -> host.scheduleCurrentProgram(channelItem));
        options.add(context.getString(R.string.menu_record_next_program));
        actions.add(() -> host.scheduleNextProgram(channelItem));
        options.add(context.getString(R.string.menu_create_current_reminder));
        actions.add(() -> host.createCurrentReminder(channelItem));
        options.add(context.getString(R.string.menu_create_next_reminder));
        actions.add(() -> host.createNextReminder(channelItem));
        options.add(context.getString(R.string.menu_view_recordings));
        actions.add(host::openRecordings);

        new AlertDialog.Builder(context)
                .setTitle(channelItem.name)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which >= 0 && which < actions.size()) {
                        actions.get(which).run();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    void showProgramActionMenu(ChannelItem channelItem, EpgRepository.EpgProgram program) {
        if (channelItem == null || program == null) {
            return;
        }

        String title = program.title == null || program.title.trim().isEmpty() ? context.getString(R.string.label_program_default) : program.title;
        boolean scheduled = host.isProgramScheduled(channelItem, program);
        Log.i(TAG, "showProgramActionMenu channel=" + channelItem.id + " scheduled=" + scheduled + " title=" + title);
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        options.add(context.getString(R.string.menu_tune_channel));
        actions.add(() -> host.tuneChannel(channelItem));
        options.add(context.getString(scheduled ? R.string.recording_action_cancel : R.string.menu_record));
        actions.add(() -> {
            if (scheduled) {
                host.cancelScheduledProgram(channelItem, program);
            } else {
                host.scheduleProgram(channelItem, program);
            }
        });
        options.add(context.getString(R.string.menu_reminder));
        actions.add(() -> host.createReminder(channelItem, program));
        options.add(context.getString(R.string.menu_playback_mode_temporary));
        actions.add(() -> host.openPlaybackModeSelector(channelItem));
        options.add(context.getString(R.string.menu_mini_guide));
        actions.add(() -> host.openMiniGuide(channelItem));
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    Log.i(TAG, "program action selected index=" + which + " channel=" + channelItem.id + " scheduled=" + scheduled);
                    if (which >= 0 && which < actions.size()) {
                        actions.get(which).run();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}
