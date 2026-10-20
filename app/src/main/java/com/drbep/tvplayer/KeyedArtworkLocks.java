package com.drbep.tvplayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/** Entries live only while a caller owns or awaits them. No I/O under map monitor. */
final class KeyedArtworkLocks {
    private static final class Entry { int users; }
    private final Map<String,Entry> entries = new HashMap<>();

    <T> T run(String key, Callable<T> work) throws Exception {
        final Entry entry;
        synchronized (this) {
            entry = entries.computeIfAbsent(key, ignored -> new Entry());
            entry.users++;
        }
        try {
            synchronized (entry) { return work.call(); }
        } finally {
            synchronized (this) {
                if (--entry.users == 0) entries.remove(key);
            }
        }
    }

    synchronized int size() { return entries.size(); }
}
