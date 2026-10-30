package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** UI-thread read snapshot only. Writers still parse their own mutable copy. */
final class HomeLiveOrderSnapshot {
    private String scope, raw;
    private JSONObject parsed;

    private static final class CleanOrder {
        final int limit;
        final List<String> ids;
        CleanOrder(int limit, List<String> ids) { this.limit = limit; this.ids = ids; }
    }
    private final Map<String, CleanOrder> cleanOrders = new LinkedHashMap<String, CleanOrder>(4, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, CleanOrder> entry) { return size() > 4; }
    };

    List<String> orderedIds(String nextScope, String nextRaw, String key, int limit) {
        JSONArray values = ids(nextScope, nextRaw, key);
        int boundedLimit = Math.min(10000, Math.max(0, limit));
        CleanOrder cached = cleanOrders.get(key);
        if (cached == null || cached.limit != boundedLimit) {
            List<String> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            if (values != null) for (int i = 0; i < values.length() && out.size() < boundedLimit; i++) {
                String value = values.optString(i, "").trim();
                if (!value.isEmpty() && seen.add(value)) out.add(value);
            }
            cached = new CleanOrder(boundedLimit, out);
            cleanOrders.put(key, cached);
        }
        // Callers retain their previous mutable-list contract without corrupting the snapshot.
        return new ArrayList<>(cached.ids);
    }

    JSONArray ids(String nextScope, String nextRaw, String key) {
        if (parsed == null || !nextScope.equals(scope) || !nextRaw.equals(raw)) {
            cleanOrders.clear();
            scope = nextScope;
            raw = nextRaw;
            try { parsed = new JSONObject(nextRaw); }
            catch (Exception malformed) { parsed = new JSONObject(); }
        }
        JSONObject entry = parsed.optJSONObject(key);
        return entry == null ? null : entry.optJSONArray("ids");
    }
}
