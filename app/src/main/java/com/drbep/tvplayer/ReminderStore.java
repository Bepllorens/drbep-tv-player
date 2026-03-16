package com.drbep.tvplayer;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class ReminderStore {
    private static final String TAG = "ReminderStore";

    static final class ReminderItem {
        final String channelId;
        final String channelName;
        final String title;
        final long startAtMillis;
        boolean notified;

        ReminderItem(String channelId, String channelName, String title, long startAtMillis, boolean notified) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.title = title;
            this.startAtMillis = startAtMillis;
            this.notified = notified;
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
                        item.optBoolean("notified", false)
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
        reminders.add(item);
        save();
    }

    List<ReminderItem> collectDueNotifications(long nowMillis) {
        boolean changed = false;
        List<ReminderItem> dueItems = new ArrayList<>();
        List<ReminderItem> toRemove = new ArrayList<>();

        for (ReminderItem item : reminders) {
            if (item == null) {
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
                arr.put(jsonObject);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(preferenceKey, arr.toString()).apply();
    }
}