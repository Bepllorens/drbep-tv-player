package com.drbep.tvplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public final class ReminderActionReceiver extends BroadcastReceiver {
    static final String ACTION_DISMISS = "com.drbep.tvplayer.REMINDER_DISMISS";
    static final String EXTRA_NOTIFICATION_ID = "notification_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_DISMISS.equals(intent.getAction())) return;
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0));
    }
}
