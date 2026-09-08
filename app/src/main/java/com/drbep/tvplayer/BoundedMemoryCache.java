package com.drbep.tvplayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

/** Synchronized LRU with an entry and aggregate-weight budget. */
final class BoundedMemoryCache<K, V> {
    private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private final int maxEntries, maxWeight;
    private final ToIntFunction<V> weigh;
    private long weight;

    BoundedMemoryCache(int maxEntries, int maxWeight, ToIntFunction<V> weigh) {
        if (maxEntries <= 0 || maxWeight <= 0 || weigh == null) throw new IllegalArgumentException();
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.weigh = weigh;
    }

    synchronized V get(K key) {
        Entry<V> entry = entries.get(key);
        return entry == null ? null : entry.value;
    }

    synchronized void put(K key, V value) {
        int nextWeight = Math.max(1, weigh.applyAsInt(value));
        Entry<V> previous = entries.remove(key);
        if (previous != null) weight -= previous.weight;
        // Never truncate a result to make it cacheable: return it uncached instead.
        if (nextWeight > maxWeight) return;
        entries.put(key, new Entry<>(value, nextWeight));
        weight += nextWeight;
        while (entries.size() > maxEntries || weight > maxWeight) {
            Map.Entry<K, Entry<V>> oldest = entries.entrySet().iterator().next();
            weight -= oldest.getValue().weight;
            entries.remove(oldest.getKey());
        }
    }

    synchronized int size() { return entries.size(); }
    synchronized long weight() { return weight; }

    private static final class Entry<V> {
        final V value;
        final int weight;
        Entry(V value, int weight) { this.value = value; this.weight = weight; }
    }
}
