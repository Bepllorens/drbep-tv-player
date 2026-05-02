package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class PlaybackDiagnosticsStore {
    static final class ErrorRecord {
        final String channelId;
        final String channelName;
        final String message;
        final String routeLabel;
        final String playbackMode;
        final long timestampMs;

        ErrorRecord(String channelId, String channelName, String message, String routeLabel, String playbackMode, long timestampMs) {
            this.channelId = channelId == null ? "" : channelId.trim();
            this.channelName = channelName == null ? "" : channelName.trim();
            this.message = message == null ? "" : message.trim();
            this.routeLabel = routeLabel == null ? "" : routeLabel.trim();
            this.playbackMode = playbackMode == null ? "" : playbackMode.trim();
            this.timestampMs = Math.max(0L, timestampMs);
        }

        String shortLabel() {
            String time = timestampMs <= 0L ? "--:--" : new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestampMs));
            return time + "  ·  " + message;
        }
    }

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final Map<String, ErrorRecord> records = new HashMap<>();
    private static final int MAX_RECORDS = 20;

    PlaybackDiagnosticsStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        records.clear();
        if (prefs == null) {
            return;
        }
        String raw = prefs.getString(preferenceKey, "{}");
        if (raw == null || raw.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(raw);
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String channelId = keys.next();
                JSONObject row = root.optJSONObject(channelId);
                if (row == null) {
                    continue;
                }
                records.put(channelId, new ErrorRecord(
                        channelId,
                        row.optString("channel_name", ""),
                        row.optString("message", ""),
                        row.optString("route", ""),
                        row.optString("mode", ""),
                        row.optLong("at", 0L)
                ));
            }
        } catch (Exception ignored) {
            records.clear();
        }
    }

    void recordError(String channelId, String channelName, String message, String routeLabel, String playbackMode) {
        if (channelId == null || channelId.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return;
        }
        records.put(channelId.trim(), new ErrorRecord(channelId, channelName, message, routeLabel, playbackMode, System.currentTimeMillis()));
        trimOldRecords();
        save();
    }

    ErrorRecord getLastError(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return null;
        }
        return records.get(channelId.trim());
    }

    void clear(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        if (records.remove(channelId.trim()) != null) {
            save();
        }
    }

    List<ErrorRecord> getRecentErrors(int maxItems) {
        List<ErrorRecord> result = new ArrayList<>(records.values());
        result.sort(Comparator.comparingLong((ErrorRecord record) -> record.timestampMs).reversed());
        if (maxItems > 0 && result.size() > maxItems) {
            return new ArrayList<>(result.subList(0, maxItems));
        }
        return result;
    }

    void clearAll() {
        if (!records.isEmpty()) {
            records.clear();
            save();
        }
    }

    private void trimOldRecords() {
        if (records.size() <= MAX_RECORDS) {
            return;
        }
        List<ErrorRecord> sorted = new ArrayList<>(records.values());
        sorted.sort(Comparator.comparingLong((ErrorRecord record) -> record.timestampMs).reversed());
        records.clear();
        int limit = Math.min(MAX_RECORDS, sorted.size());
        for (int i = 0; i < limit; i++) {
            ErrorRecord record = sorted.get(i);
            records.put(record.channelId, record);
        }
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, ErrorRecord> entry : records.entrySet()) {
                ErrorRecord record = entry.getValue();
                if (record == null) {
                    continue;
                }
                JSONObject row = new JSONObject();
                row.put("channel_name", record.channelName);
                row.put("message", record.message);
                row.put("route", record.routeLabel);
                row.put("mode", record.playbackMode);
                row.put("at", record.timestampMs);
                root.put(entry.getKey(), row);
            }
            prefs.edit().putString(preferenceKey, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
