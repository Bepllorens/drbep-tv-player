package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ChannelCollectionStore {
    static final class ChannelCollection {
        final String key;
        final String label;
        final Set<String> channelIds;

        ChannelCollection(String key, String label, Set<String> channelIds) {
            this.key = key;
            this.label = label;
            this.channelIds = channelIds;
        }
    }

    private static final String[] DEFAULT_LABELS = new String[]{"Deportes", "Noticias", "Kids", "Mis canales"};

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final LinkedHashMap<String, ChannelCollection> collections = new LinkedHashMap<>();

    ChannelCollectionStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
        ensureDefaults();
    }

    void load() {
        collections.clear();
        if (prefs == null) {
            ensureDefaults();
            return;
        }
        String raw = prefs.getString(preferenceKey, "{}");
        if (raw == null || raw.trim().isEmpty()) {
            ensureDefaults();
            return;
        }
        try {
            JSONObject root = new JSONObject(raw);
            JSONArray rows = root.optJSONArray("collections");
            if (rows == null) {
                ensureDefaults();
                return;
            }
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) {
                    continue;
                }
                String label = row.optString("label", "").trim();
                String key = normalizeKey(row.optString("key", label));
                if (key.isEmpty() || label.isEmpty()) {
                    continue;
                }
                Set<String> ids = new HashSet<>();
                JSONArray items = row.optJSONArray("channel_ids");
                if (items != null) {
                    for (int j = 0; j < items.length(); j++) {
                        String channelId = items.optString(j, "").trim();
                        if (!channelId.isEmpty()) {
                            ids.add(channelId);
                        }
                    }
                }
                collections.put(key, new ChannelCollection(key, label, ids));
            }
            if (collections.isEmpty()) {
                ensureDefaults();
            }
        } catch (Exception ignored) {
            collections.clear();
            ensureDefaults();
        }
    }

    List<ChannelCollection> getCollections() {
        return new ArrayList<>(collections.values());
    }

    List<ChannelCollection> getNonEmptyCollections() {
        List<ChannelCollection> result = new ArrayList<>();
        for (ChannelCollection collection : collections.values()) {
            if (!collection.channelIds.isEmpty()) {
                result.add(collection);
            }
        }
        return result;
    }

    ChannelCollection getCollection(String collectionKey) {
        return collections.get(normalizeKey(collectionKey));
    }

    List<String> getMembershipLabels(String channelId, int maxLabels) {
        List<String> labels = new ArrayList<>();
        if (channelId == null || channelId.trim().isEmpty() || maxLabels <= 0) {
            return labels;
        }
        for (ChannelCollection collection : collections.values()) {
            if (collection.channelIds.contains(channelId)) {
                labels.add(collection.label);
                if (labels.size() >= maxLabels) {
                    break;
                }
            }
        }
        return labels;
    }

    boolean contains(String collectionKey, String channelId) {
        ChannelCollection collection = collections.get(normalizeKey(collectionKey));
        return collection != null && channelId != null && collection.channelIds.contains(channelId);
    }

    boolean hasAnyMembership(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return false;
        }
        for (ChannelCollection collection : collections.values()) {
            if (collection.channelIds.contains(channelId)) {
                return true;
            }
        }
        return false;
    }

    void setMembership(String collectionKey, String channelId, boolean enabled) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        ChannelCollection collection = collections.get(normalizeKey(collectionKey));
        if (collection == null) {
            return;
        }
        boolean changed = enabled ? collection.channelIds.add(channelId.trim()) : collection.channelIds.remove(channelId.trim());
        if (changed) {
            save();
        }
    }

    ChannelCollection createCollection(String label) {
        String trimmed = label == null ? "" : label.trim();
        String key = uniqueKey(trimmed);
        if (trimmed.isEmpty() || key.isEmpty()) {
            return null;
        }
        ChannelCollection collection = new ChannelCollection(key, trimmed, new HashSet<>());
        collections.put(key, collection);
        save();
        return collection;
    }

    boolean renameCollection(String collectionKey, String label) {
        String key = normalizeKey(collectionKey);
        ChannelCollection current = collections.get(key);
        String trimmed = label == null ? "" : label.trim();
        if (current == null || trimmed.isEmpty()) {
            return false;
        }
        collections.put(key, new ChannelCollection(key, trimmed, new HashSet<>(current.channelIds)));
        save();
        return true;
    }

    boolean deleteCollection(String collectionKey) {
        String key = normalizeKey(collectionKey);
        if (key.isEmpty() || collections.remove(key) == null) {
            return false;
        }
        save();
        return true;
    }

    private void ensureDefaults() {
        for (String label : DEFAULT_LABELS) {
            String key = normalizeKey(label);
            if (!collections.containsKey(key)) {
                collections.put(key, new ChannelCollection(key, label, new HashSet<>()));
            }
        }
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        try {
            JSONArray rows = new JSONArray();
            for (Map.Entry<String, ChannelCollection> entry : collections.entrySet()) {
                ChannelCollection collection = entry.getValue();
                JSONObject row = new JSONObject();
                row.put("key", collection.key);
                row.put("label", collection.label);
                JSONArray ids = new JSONArray();
                List<String> sortedIds = new ArrayList<>(collection.channelIds);
                sortedIds.sort(String::compareTo);
                for (String channelId : sortedIds) {
                    ids.put(channelId);
                }
                row.put("channel_ids", ids);
                rows.put(row);
            }
            JSONObject root = new JSONObject();
            root.put("collections", rows);
            prefs.edit().putString(preferenceKey, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private String uniqueKey(String label) {
        String base = normalizeKey(label);
        if (base.isEmpty()) {
            return "";
        }
        String key = base;
        int suffix = 2;
        while (collections.containsKey(key)) {
            key = base + "-" + suffix;
            suffix++;
        }
        return key;
    }
}
