package com.drbep.tvplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Trabajo que se ejecuta cuando llega la hora de un recordatorio. Publica una
 * notificacion del sistema (persistente frente al cierre de la app) y marca el
 * recordatorio como notificado en el almacen compartido.
 */
public final class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";
    static final String NOTIFICATION_CHANNEL_ID = "drbep_reminders";
    private static final String NOTIFICATION_CHANNEL_NAME = "Recordatorios de programas";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String channelId = getInputData().getString(ReminderScheduler.KEY_CHANNEL_ID);
        String channelName = getInputData().getString(ReminderScheduler.KEY_CHANNEL_NAME);
        String title = getInputData().getString(ReminderScheduler.KEY_TITLE);
        long startAt = getInputData().getLong(ReminderScheduler.KEY_START_AT, 0L);
        long endAt = getInputData().getLong(ReminderScheduler.KEY_END_AT, 0L);

        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = context.getString(R.string.app_name);
        }
        if (title == null || title.trim().isEmpty()) {
            title = context.getString(R.string.label_program_default);
        }

        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences(
                    ReminderStore.PREFS_NAME, Context.MODE_PRIVATE);
            ReminderStore store = new ReminderStore(prefs, ReminderStore.PREF_KEY);
            store.load();
            store.markNotified(channelId, startAt);
        } catch (Exception e) {
            Log.w(TAG, "markNotified failed", e);
        }

        showNotification(context, channelId, channelName, title, startAt, endAt);
        return Result.success();
    }

    private void showNotification(Context context, String channelId, String channelName,
                                  String title, long startAt, long endAt) {
        try {
            ensureChannel(context);

            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (channelId != null && !channelId.isEmpty()) {
                launchIntent.putExtra("reminder_channel_id", channelId);
            }
            launchIntent.putExtra("reminder_action", "watch");
            launchIntent.putExtra("reminder_title", title);
            launchIntent.putExtra("reminder_start_at", startAt);
            launchIntent.putExtra("reminder_end_at", endAt);

            int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            int notificationId = buildNotificationId(channelId, startAt);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, notificationId, launchIntent, pendingFlags);

            Intent recordIntent = new Intent(launchIntent);
            recordIntent.putExtra("reminder_action", "record");
            PendingIntent recordPendingIntent = PendingIntent.getActivity(
                    context, notificationId + 1, recordIntent, pendingFlags);
            Intent dismissIntent = new Intent(context, ReminderActionReceiver.class);
            dismissIntent.setAction(ReminderActionReceiver.ACTION_DISMISS);
            dismissIntent.putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                    context, notificationId + 2, dismissIntent, pendingFlags);

            String contentText = context.getString(
                    R.string.status_reminder_due, channelName, title);

            Notification notification = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.drbep_launcher_icon)
                    .setContentTitle(channelName)
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .addAction(0, context.getString(R.string.reminder_action_watch), contentIntent)
                    .addAction(0, context.getString(R.string.reminder_action_record), recordPendingIntent)
                    .addAction(0, context.getString(R.string.reminder_action_dismiss), dismissPendingIntent)
                    .build();

            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                manager.notify(notificationId, notification);
            }
        } catch (Exception e) {
            Log.w(TAG, "showNotification failed", e);
        }
    }

    private void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Avisos cuando empieza un programa con recordatorio.");
        manager.createNotificationChannel(channel);
    }

    private int buildNotificationId(String channelId, long startAt) {
        String key = (channelId == null ? "" : channelId) + "_" + startAt;
        return key.hashCode();
    }
}
