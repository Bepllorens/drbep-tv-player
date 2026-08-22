package com.drbep.tvplayer;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Programa recordatorios de programas con WorkManager, de modo que la notificacion
 * se dispare aunque la app este cerrada o el dispositivo se reinicie (WorkManager
 * persiste su propia cola de trabajos).
 */
final class ReminderScheduler {

    static final String WORK_PREFIX = "reminder_";
    static final String WORK_TAG = "drbep_reminder";

    // Si el inicio ya paso hace mas de este margen, no tiene sentido notificar.
    private static final long STALE_THRESHOLD_MS = 60L * 1000L;

    static final String KEY_CHANNEL_ID = "channel_id";
    static final String KEY_CHANNEL_NAME = "channel_name";
    static final String KEY_TITLE = "title";
    static final String KEY_START_AT = "start_at";
    static final String KEY_END_AT = "end_at";

    private ReminderScheduler() {
    }

    static String workName(String channelId, long startAtMillis) {
        return WORK_PREFIX + (channelId == null ? "" : channelId) + "_" + startAtMillis;
    }

    /** Programa un unico recordatorio. Reemplaza cualquier trabajo previo con la misma clave. */
    static void schedule(Context context, ReminderStore.ReminderItem item) {
        if (context == null || item == null || item.startAtMillis <= 0L) {
            return;
        }
        long delay = item.startAtMillis - System.currentTimeMillis();
        if (delay < -STALE_THRESHOLD_MS) {
            // Programa ya empezado hace rato: no reprogramar.
            return;
        }
        long initialDelay = Math.max(0L, delay);

        Data data = new Data.Builder()
                .putString(KEY_CHANNEL_ID, item.channelId)
                .putString(KEY_CHANNEL_NAME, item.channelName)
                .putString(KEY_TITLE, item.title)
                .putLong(KEY_START_AT, item.startAtMillis)
                .putLong(KEY_END_AT, item.endAtMillis)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(WORK_TAG)
                .build();

        try {
            WorkManager.getInstance(context.getApplicationContext())
                    .enqueueUniqueWork(
                            workName(item.channelId, item.startAtMillis),
                            ExistingWorkPolicy.REPLACE,
                            request);
        } catch (Exception e) {
            Log.w("ReminderScheduler", "schedule failed", e);
        }
    }

    /** Reprograma todos los recordatorios pendientes (arranque de app o migracion de datos previos). */
    static void reschedulePending(Context context, ReminderStore store) {
        if (context == null || store == null) {
            return;
        }
        List<ReminderStore.ReminderItem> pending = store.getPendingReminders();
        for (ReminderStore.ReminderItem item : pending) {
            schedule(context, item);
        }
    }

    static void cancel(Context context, String channelId, long startAtMillis) {
        if (context == null) {
            return;
        }
        try {
            WorkManager.getInstance(context.getApplicationContext())
                    .cancelUniqueWork(workName(channelId, startAtMillis));
        } catch (Exception e) {
            Log.w("ReminderScheduler", "cancel failed", e);
        }
    }
}
