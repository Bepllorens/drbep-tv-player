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
    public synchronized void save(String channel, String day, String rowKey) {
        entries.remove(channel);
        entries.put(channel, new Selection(day, rowKey));
        while (entries.size() > 16) entries.remove(entries.keySet().iterator().next());
    }
}
