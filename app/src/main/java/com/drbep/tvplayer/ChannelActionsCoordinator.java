package com.drbep.tvplayer;

import android.app.AlertDialog;
import android.content.Context;

final class ChannelActionsCoordinator {
    interface Host {
        void tuneSelectedChannel();

        void toggleFavoriteSelected();

        void openMiniGuide(ChannelItem channelItem);

        void scheduleCurrentProgram(ChannelItem channelItem);

        void scheduleNextProgram(ChannelItem channelItem);

        void createCurrentReminder(ChannelItem channelItem);

        void createNextReminder(ChannelItem channelItem);

        void openRecordings();

        void scheduleProgram(ChannelItem channelItem, EpgRepository.EpgProgram program);

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

        String[] options = context.getResources().getStringArray(
            favorite ? R.array.channel_action_menu_favorite_on : R.array.channel_action_menu_favorite_off
        );

        new AlertDialog.Builder(context)
                .setTitle(channelItem.name)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            host.tuneSelectedChannel();
                            break;
                        case 1:
                            host.toggleFavoriteSelected();
                            break;
                        case 2:
                            host.openMiniGuide(channelItem);
                            break;
                        case 3:
                            host.scheduleCurrentProgram(channelItem);
                            break;
                        case 4:
                            host.scheduleNextProgram(channelItem);
                            break;
                        case 5:
                            host.createCurrentReminder(channelItem);
                            break;
                        case 6:
                            host.createNextReminder(channelItem);
                            break;
                        case 7:
                            host.openRecordings();
                            break;
                        default:
                            break;
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
        String[] options = context.getResources().getStringArray(R.array.program_action_menu_options);
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        host.scheduleProgram(channelItem, program);
                    } else if (which == 1) {
                        host.createReminder(channelItem, program);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }
}