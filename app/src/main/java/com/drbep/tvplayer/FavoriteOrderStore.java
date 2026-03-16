package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

final class FavoriteOrderStore {
    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final List<String> orderedIds = new ArrayList<>();

    FavoriteOrderStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        orderedIds.clear();
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
                String channelId = array.optString(i, "").trim();
                if (!channelId.isEmpty() && !orderedIds.contains(channelId)) {
                    orderedIds.add(channelId);
                }
            }
        } catch (Exception ignored) {
        }
    }

    List<String> getOrderedIds() {
        return orderedIds;
    }

    void addIfMissing(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        if (!orderedIds.contains(channelId)) {
            orderedIds.add(channelId);
            save();
        }
    }

    void remove(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        if (orderedIds.remove(channelId)) {
            save();
        }
    }

    boolean move(String channelId, int delta) {
        if (channelId == null || channelId.trim().isEmpty() || delta == 0) {
            return false;
        }
        int index = orderedIds.indexOf(channelId);
        if (index < 0) {
            return false;
        }
        int next = index + delta;
        if (next < 0 || next >= orderedIds.size()) {
            return false;
        }
        orderedIds.remove(index);
        orderedIds.add(next, channelId);
        save();
        return true;
    }

    void syncToFavorites(Iterable<String> favoriteIds) {
        List<String> valid = new ArrayList<>();
        if (favoriteIds != null) {
            for (String favoriteId : favoriteIds) {
                if (favoriteId != null && !favoriteId.trim().isEmpty() && orderedIds.contains(favoriteId) && !valid.contains(favoriteId)) {
                    valid.add(favoriteId);
                }
            }
            for (String favoriteId : favoriteIds) {
                if (favoriteId != null && !favoriteId.trim().isEmpty() && !valid.contains(favoriteId)) {
                    valid.add(favoriteId);
                }
            }
        }
        orderedIds.clear();
        orderedIds.addAll(valid);
        save();
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (String orderedId : orderedIds) {
            array.put(orderedId);
        }
        prefs.edit().putString(preferenceKey, array.toString()).apply();
    }
}