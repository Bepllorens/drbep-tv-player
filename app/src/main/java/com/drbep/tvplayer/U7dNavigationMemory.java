package com.drbep.tvplayer;

import java.util.LinkedHashMap;
import java.util.Map;

/** UI position only; never retains a Context, artwork or playback callback. */
public final class U7dNavigationMemory {
    private final Map<String, Selection> entries = new LinkedHashMap<>();
    public static final class Selection {
        public final String day, rowKey;
        Selection(String day, String rowKey) { this.day = day; this.rowKey = rowKey; }
    }
    public synchronized Selection get(String channel) { return entries.get(channel); }
    private final Map<String, Map<String, String>> dayRows = new LinkedHashMap<>();
    public synchronized String rowForDay(String channel, String day) {
        Map<String, String> rows = dayRows.get(channel);
        return rows == null ? "" : rows.getOrDefault(day, "");
    }
    public synchronized void save(String channel, String day, String rowKey) {
        entries.remove(channel);
        entries.put(channel, new Selection(day, rowKey));
        Map<String, String> rows = dayRows.computeIfAbsent(channel, key -> new LinkedHashMap<>());
        if (rowKey != null && !rowKey.isEmpty()) rows.put(day, rowKey);
        while (rows.size() > 8) rows.remove(rows.keySet().iterator().next());
        while (entries.size() > 16) {
            String oldest = entries.keySet().iterator().next();
            entries.remove(oldest);
            dayRows.remove(oldest);
        }
    }
}
