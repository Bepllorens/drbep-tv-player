package com.drbep.tvplayer;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ReminderStore {
    private static final String TAG = "ReminderStore";

    // Constantes compartidas para que el Worker de WorkManager pueda abrir el
    // mismo almacen de recordatorios sin depender de MainActivity.
    static final String PREFS_NAME = "drbep_tv_prefs";
    static final String PREF_KEY = "channel_reminders";

    static final class ReminderItem {
        final String channelId;
        final String channelName;
        final String title;
        final long startAtMillis;
        boolean notified;
        long updatedAtMillis;
        long deletedAtMillis;

        ReminderItem(String channelId, String channelName, String title, long startAtMillis, boolean notified) {
            this(channelId, channelName, title, startAtMillis, notified, System.currentTimeMillis(), 0L);
        }

        ReminderItem(String channelId, String channelName, String title, long startAtMillis, boolean notified, long updatedAtMillis, long deletedAtMillis) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.title = title;
            this.startAtMillis = startAtMillis;
            this.notified = notified;
            this.updatedAtMillis = Math.max(1L, updatedAtMillis);
            this.deletedAtMillis = Math.max(0L, deletedAtMillis);
        }
    }

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final List<ReminderItem> reminders = new ArrayList<>();

    ReminderStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        reminders.clear();
        if (prefs == null) {
            return;
        }

        String raw = prefs.getString(preferenceKey, "[]");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }

        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                reminders.add(new ReminderItem(
                        item.optString("channel_id", ""),
                        item.optString("channel_name", ""),
                        item.optString("title", "Programa"),
                        item.optLong("start_at", 0L),
                        item.optBoolean("notified", false),
                        item.optLong("updated_at", System.currentTimeMillis()),
                        item.optLong("deleted_at", 0L)
                ));
            }
        } catch (Exception e) {
            Log.w(TAG, "load reminders failed", e);
        }
    }

    void addReminder(ReminderItem item) {
        if (item == null) {
            return;
        }
        String key = reminderKey(item.channelId, item.startAtMillis);
        reminders.removeIf(existing -> key.equals(reminderKey(existing.channelId, existing.startAtMillis)));
        item.updatedAtMillis = System.currentTimeMillis();
        item.deletedAtMillis = 0L;
        reminders.add(item);
        save();
    }

    /** Recordatorios aun no notificados, para reprogramarlos en WorkManager al arrancar. */
    List<ReminderItem> getPendingReminders() {
        List<ReminderItem> out = new ArrayList<>();
        for (ReminderItem item : reminders) {
            if (item != null && item.deletedAtMillis <= 0L && !item.notified) {
                out.add(item);
            }
        }
        return out;
    }

    /** Marca como notificado el recordatorio disparado por el Worker y persiste el cambio. */
    void markNotified(String channelId, long startAtMillis) {
        boolean changed = false;
        for (ReminderItem item : reminders) {
            if (item != null && !item.notified
                    && channelId != null && channelId.equals(item.channelId)
                    && item.startAtMillis == startAtMillis) {
                item.notified = true;
                item.updatedAtMillis = System.currentTimeMillis();
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    List<ReminderItem> collectDueNotifications(long nowMillis) {
        boolean changed = false;
        List<ReminderItem> dueItems = new ArrayList<>();
        List<ReminderItem> toRemove = new ArrayList<>();

        for (ReminderItem item : reminders) {
            if (item == null) {
                continue;
            }
            if (item.deletedAtMillis > 0L) {
                if (nowMillis - item.deletedAtMillis > 30L * 24L * 60L * 60L * 1000L) {
                    toRemove.add(item);
                    changed = true;
                }
                continue;
            }
            if (item.notified) {
                if (nowMillis > item.startAtMillis + 10 * 60 * 1000L) {
                    toRemove.add(item);
                    changed = true;
                }
                continue;
            }

            long delta = item.startAtMillis - nowMillis;
            if (delta <= 60 * 1000L && delta >= -60 * 1000L) {
                item.notified = true;
                item.updatedAtMillis = nowMillis;
                dueItems.add(item);
                changed = true;
            }
        }

        if (!toRemove.isEmpty()) {
            reminders.removeAll(toRemove);
        }
        if (changed) {
            save();
        }
        return dueItems;
    }

    JSONObject toRemoteJson() {
        JSONObject out = new JSONObject();
        for (ReminderItem item : reminders) {
            if (item == null) {
                continue;
            }
            try {
                JSONObject row = new JSONObject();
                row.put("updated_at", Math.max(1L, item.updatedAtMillis));
                if (item.deletedAtMillis > 0L) {
                    row.put("deleted_at", item.deletedAtMillis);
                } else {
                    row.put("channel_id", item.channelId);
                    row.put("channel_name", item.channelName);
                    row.put("title", item.title);
                    row.put("start_at", item.startAtMillis);
                    row.put("notified", item.notified);
                }
                out.put(reminderKey(item.channelId, item.startAtMillis), row);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    boolean mergeRemote(JSONObject remote) {
        if (remote == null) {
            return false;
        }
        boolean changed = false;
        java.util.Iterator<String> keys = remote.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONObject row = remote.optJSONObject(key);
            if (row == null) {
                continue;
            }
            long updatedAt = Math.max(row.optLong("updated_at", 0L), row.optLong("deleted_at", 0L));
            if (updatedAt <= 0L) {
                continue;
            }
            ReminderItem current = null;
            for (ReminderItem item : reminders) {
                if (item != null && key.equals(reminderKey(item.channelId, item.startAtMillis))) {
                    current = item;
                    break;
                }
            }
            if (current != null && current.updatedAtMillis > updatedAt) {
                continue;
            }
            if (current != null) {
                reminders.remove(current);
            }
            long deletedAt = row.optLong("deleted_at", 0L);
            String channelId = row.optString("channel_id", "");
            long startAt = row.optLong("start_at", 0L);
            if (deletedAt > 0L && (channelId.isEmpty() || startAt <= 0L)) {
                int split = key.lastIndexOf('|');
                if (split > 0) {
                    channelId = key.substring(0, split);
                    try {
                        startAt = Long.parseLong(key.substring(split + 1));
                    } catch (Exception ignored) {
                        startAt = 0L;
                    }
                }
            }
            if (!channelId.isEmpty() && startAt > 0L) {
                reminders.add(new ReminderItem(
                        channelId,
                        row.optString("channel_name", ""),
                        row.optString("title", "Programa"),
                        startAt,
                        row.optBoolean("notified", false),
                        updatedAt,
                        deletedAt
                ));
                changed = true;
            }
        }
        if (changed) {
            save();
        }
        return changed;
    }

    private static String reminderKey(String channelId, long startAtMillis) {
        return (channelId == null ? "" : channelId.trim()) + "|" + Math.max(0L, startAtMillis);
    }

    private void save() {
        if (prefs == null) {
            return;
        }

        JSONArray arr = new JSONArray();
        for (ReminderItem item : reminders) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("channel_id", item.channelId);
                jsonObject.put("channel_name", item.channelName);
                jsonObject.put("title", item.title);
                jsonObject.put("start_at", item.startAtMillis);
                jsonObject.put("notified", item.notified);
                jsonObject.put("updated_at", Math.max(1L, item.updatedAtMillis));
                if (item.deletedAtMillis > 0L) {
                    jsonObject.put("deleted_at", item.deletedAtMillis);
                }
                arr.put(jsonObject);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(preferenceKey, arr.toString()).apply();
    }
}
