package com.drbep.tvplayer;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.RandomAccess;

/** Catalogue with bounded lookups; every list mutation invalidates hits and misses. */
final class ChannelLookupList extends AbstractList<ChannelItem> implements RandomAccess {
    private final ArrayList<ChannelItem> items = new ArrayList<>();
    private final Map<String, ChannelItem> recent = new LinkedHashMap<String, ChannelItem>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, ChannelItem> entry) { return size() > 128; }
    };
    @Override public ChannelItem get(int index) { return items.get(index); }
    @Override public int size() { return items.size(); }
    @Override public synchronized ChannelItem set(int index, ChannelItem value) {
        ChannelItem previous = items.set(index, value); recent.clear(); return previous;
    }
    @Override public synchronized void add(int index, ChannelItem value) {
        items.add(index, value); modCount++; recent.clear();
    }
    @Override public synchronized boolean addAll(Collection<? extends ChannelItem> values) {
        return addAll(items.size(), values);
    }
    @Override public synchronized boolean addAll(int index, Collection<? extends ChannelItem> values) {
        boolean changed = items.addAll(index, values);
        if (changed) { modCount++; recent.clear(); }
        return changed;
    }
    @Override public synchronized ChannelItem remove(int index) {
        ChannelItem previous = items.remove(index); modCount++; recent.clear(); return previous;
    }
    @Override protected synchronized void removeRange(int from, int to) {
        if (from == to) return;
        items.subList(from, to).clear(); modCount++; recent.clear();
    }
    synchronized ChannelItem find(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        if (recent.containsKey(id)) return recent.get(id);
        ChannelItem found = null;
        for (ChannelItem item : items) if (item != null && id.equals(item.id)) { found = item; break; }
        recent.put(id, found);
        return found;
    }
}
