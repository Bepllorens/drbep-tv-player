package com.drbep.tvplayer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/** One queued/running load per key. Followers never occupy worker threads. */
final class ArtworkSingleFlight<K,V> implements AutoCloseable {
    private final Executor executor;
    private final int limit;
    private final Map<K,List<Consumer<V>>> pending = new HashMap<>();
    private int waiters;
    private boolean closed;

    ArtworkSingleFlight(Executor executor, int limit) {
        if (limit < 1) throw new IllegalArgumentException();
        this.executor = executor;
        this.limit = limit;
    }

    boolean request(K key, Callable<V> loader, Consumer<V> callback) {
        List<Consumer<V>> group;
        synchronized (this) {
            if (closed || waiters >= limit) return false;
            group = pending.get(key);
            if (group != null) {
                group.add(callback);
                waiters++;
                return true;
            }
            group = new ArrayList<>();
            group.add(callback);
            pending.put(key, group);
            waiters++;
        }
        final List<Consumer<V>> owned = group;
        try {
            executor.execute(() -> {
                synchronized (this) {
                    if (closed || pending.get(key) != owned) return;
                }
                V value = null;
                try { value = loader.call(); }
                catch (Exception ignored) { /* Existing image fallback handles missing results. */ }
                complete(key, owned, value);
            });
            return true;
        } catch (RejectedExecutionException rejected) {
            complete(key, owned, null);
            return false;
        }
    }

    private void complete(K key, List<Consumer<V>> owned, V value) {
        List<Consumer<V>> callbacks;
        synchronized (this) {
            if (pending.get(key) != owned) return;
            pending.remove(key);
            waiters -= owned.size();
            callbacks = new ArrayList<>(owned);
        }
        for (Consumer<V> callback : callbacks) {
            synchronized (this) { if (closed) return; }
            try { callback.accept(value); }
            catch (RuntimeException ignored) { /* One abandoned view cannot starve its peers. */ }
        }
    }

    @Override public synchronized void close() {
        closed = true;
        pending.clear();
        waiters = 0;
    }
}
