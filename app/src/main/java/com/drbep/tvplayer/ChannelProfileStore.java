package com.drbep.tvplayer;

import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class ChannelProfileStore {
    static final class ChannelProfile {
        final String alias;
        final String tag;
        final boolean hidden;

        ChannelProfile(String alias, String tag, boolean hidden) {
            this.alias = alias == null ? "" : alias.trim();
            this.tag = tag == null ? "" : tag.trim();
            this.hidden = hidden;
        }
    }

    private final SharedPreferences prefs;
    private final String preferenceKey;
    private final Map<String, ChannelProfile> values = new HashMap<>();

    ChannelProfileStore(SharedPreferences prefs, String preferenceKey) {
        this.prefs = prefs;
        this.preferenceKey = preferenceKey;
    }

    void load() {
        values.clear();
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
                JSONObject profile = root.optJSONObject(channelId);
                if (profile != null) {
                    values.put(channelId, new ChannelProfile(profile.optString("alias", ""), profile.optString("tag", ""), profile.optBoolean("hidden", false)));
                }
            }
        } catch (Exception ignored) {
            values.clear();
        }
    }

    ChannelProfile getProfile(String channelId) {
        ChannelProfile profile = getProfileValue(channelId);
        if (profile == null) {
            return new ChannelProfile("", "", false);
        }
        return profile;
    }

    String getDisplayName(String channelId, String fallbackName) {
        ChannelProfile profile = getProfile(channelId);
        return profile.alias.isEmpty() ? (fallbackName == null ? "" : fallbackName) : profile.alias;
    }

    boolean hasAlias(String channelId) {
        return !getProfile(channelId).alias.isEmpty();
    }

    String getTag(String channelId) {
        return getProfile(channelId).tag;
    }

    boolean hasTag(String channelId) {
        return !getProfile(channelId).tag.isEmpty();
    }

    boolean isHidden(String channelId) {
        return getProfile(channelId).hidden;
    }

    Set<String> getHiddenChannelIds() {
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, ChannelProfile> entry : values.entrySet()) {
            ChannelProfile profile = entry.getValue();
            if (profile != null && profile.hidden) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    void setAlias(String channelId, String alias) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        try {
            String trimmed = alias == null ? "" : alias.trim();
            ChannelProfile current = getProfile(channelId);
            putOrRemove(channelId.trim(), new ChannelProfile(trimmed, current.tag, current.hidden));
        } catch (Exception ignored) {
        }
    }

    void setTag(String channelId, String tag) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        try {
            String trimmed = tag == null ? "" : tag.trim();
            ChannelProfile current = getProfile(channelId);
            putOrRemove(channelId.trim(), new ChannelProfile(current.alias, trimmed, current.hidden));
        } catch (Exception ignored) {
        }
    }

    void setHidden(String channelId, boolean hidden) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return;
        }
        try {
            ChannelProfile current = getProfile(channelId);
            putOrRemove(channelId.trim(), new ChannelProfile(current.alias, current.tag, hidden));
        } catch (Exception ignored) {
        }
    }

    private ChannelProfile getProfileValue(String channelId) {
        if (channelId == null || channelId.trim().isEmpty()) {
            return null;
        }
        return values.get(channelId.trim());
    }

    private void putOrRemove(String channelId, ChannelProfile profile) {
        if (profile.alias.isEmpty() && profile.tag.isEmpty() && !profile.hidden) {
            values.remove(channelId);
        } else {
            values.put(channelId, profile);
        }
        save();
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, ChannelProfile> entry : values.entrySet()) {
                ChannelProfile profile = entry.getValue();
                if (profile == null) {
                    continue;
                }
                JSONObject row = new JSONObject();
                if (!profile.alias.isEmpty()) {
                    row.put("alias", profile.alias);
                }
                if (!profile.tag.isEmpty()) {
                    row.put("tag", profile.tag);
                }
                if (profile.hidden) {
                    row.put("hidden", true);
                }
                if (row.length() > 0) {
                    root.put(entry.getKey(), row);
                }
            }
            prefs.edit().putString(preferenceKey, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
