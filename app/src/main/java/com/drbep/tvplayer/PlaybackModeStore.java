package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONObject;

final class PlaybackModeStore {
    static final String MODE_AUTO = "auto";
    static final String MODE_DIRECT = "direct";
    static final String MODE_PROXY = "proxy";

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private JSONObject values = new JSONObject();

    PlaybackModeStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        values = new JSONObject();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(preferenceKey, "{}");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            values = new JSONObject(raw);
        } catch (Exception ignored) {
            values = new JSONObject();
        }
    }

    String getMode(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return MODE_AUTO;
        }
        String mode = values.optString(channelId, MODE_AUTO).trim();
        if (!MODE_DIRECT.equals(mode) && !MODE_PROXY.equals(mode)) {
            return MODE_AUTO;
        }
        return mode;
    }

    void setMode(String channelId, String mode) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        try {
            if (mode == null || MODE_AUTO.equals(mode)) {
                values.remove(channelId);
            } else {
                values.put(channelId, mode);
            }
            save();
        } catch (Exception ignored) {
        }
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        prefs.edit().putString(preferenceKey, values.toString()).apply();
    }
}