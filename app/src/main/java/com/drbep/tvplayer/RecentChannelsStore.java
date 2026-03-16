package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class RecentChannelsStore {
    static final class RecentChannelItem {
        final String channelId;
        final String channelName;
        final long watchedAt;

        RecentChannelItem(String channelId, String channelName, long watchedAt) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.watchedAt = watchedAt;
        }
    }

    private static final int MAX_ITEMS = 8;

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final List<RecentChannelItem> items = new ArrayList<>();

    RecentChannelsStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        items.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(preferenceKey, "[]");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                String channelId = item.optString("channel_id", "").trim();
                String channelName = item.optString("channel_name", "").trim();
                if (channelId.isEmpty() || channelName.isEmpty()) {
                    continue;
                }
                items.add(new RecentChannelItem(channelId, channelName, item.optLong("watched_at", 0L)));
            }
        } catch (Exception ignored) {
        }
    }

    void add(String channelId, String channelName) {
        if (channelId == null || channelId.trim().isEmpty() || channelName == null || channelName.trim().isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (channelId.equals(items.get(i).channelId)) {
                items.remove(i);
                break;
            }
        }
        items.add(0, new RecentChannelItem(channelId, channelName.trim(), System.currentTimeMillis()));
        while (items.size() > MAX_ITEMS) {
            items.remove(items.size() - 1);
        }
        save();
    }

    List<RecentChannelItem> getItems() {
        return new ArrayList<>(items);
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (RecentChannelItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("channel_id", item.channelId);
                object.put("channel_name", item.channelName);
                object.put("watched_at", item.watchedAt);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        prefs.edit().putString(preferenceKey, array.toString()).apply();
    }
}