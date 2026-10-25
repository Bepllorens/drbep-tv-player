package com.drbep.tvplayer;

import org.json.JSONArray;
import org.json.JSONObject;

/** UI-thread read snapshot only. Writers still parse their own mutable copy. */
final class HomeLiveOrderSnapshot {
    private String scope, raw;
    private JSONObject parsed;

    JSONArray ids(String nextScope, String nextRaw, String key) {
        if (parsed == null || !nextScope.equals(scope) || !nextRaw.equals(raw)) {
            scope = nextScope;
            raw = nextRaw;
            try { parsed = new JSONObject(nextRaw); }
            catch (Exception malformed) { parsed = new JSONObject(); }
        }
        JSONObject entry = parsed.optJSONObject(key);
        return entry == null ? null : entry.optJSONArray("ids");
    }
}
