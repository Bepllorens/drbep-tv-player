package com.drbep.tvplayer;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/** Scoped to one library binder/session. Never fetches unselected catalog items. */
final class LibraryInfoCache {
    private final LinkedHashMap<String, String[]> cache = new LinkedHashMap<>(32, .75f, true);
    private final Map<String, List<Consumer<String[]>>> pending = new HashMap<>();
    Runnable request(String key, Supplier<String[]> loader, Consumer<String[]> ready,
                     Executor worker, Executor delivery) {
        AtomicBoolean cancelled = new AtomicBoolean();
        Consumer<String[]> listener = value -> delivery.execute(() -> {
            if (!cancelled.get()) ready.accept(value.clone());
        });
        String[] hit;
        synchronized (this) {
            hit = cache.get(key);
            if (hit == null) {
                List<Consumer<String[]>> listeners = pending.get(key);
                if (listeners != null) {
                    listeners.add(listener);
                    return () -> { cancelled.set(true); detach(key, listener); };
                }
                pending.put(key, new ArrayList<>(Collections.singletonList(listener)));
            }
        }
        if (hit != null) listener.accept(hit);
        else {
            try {
                worker.execute(() -> {
                    synchronized (this) {
                        List<Consumer<String[]>> waiting = pending.get(key);
                        if (waiting == null || waiting.isEmpty()) { pending.remove(key); return; }
                    }
                    String[] value;
                    boolean success = true;
                    try { value = loader.get(); if (value == null) throw new IllegalStateException(); }
                    catch (Exception failure) { value = new String[]{"", ""}; success = false; }
                    List<Consumer<String[]>> listeners;
                    synchronized (this) {
                        if (success) {
                            cache.put(key, value.clone());
                            while (cache.size() > 32) cache.remove(cache.keySet().iterator().next());
                        }
                        listeners = pending.remove(key);
                    }
                    if (listeners != null) for (Consumer<String[]> callback : listeners) callback.accept(value);
                });
            } catch (RuntimeException rejected) {
                List<Consumer<String[]>> listeners;
                synchronized (this) { listeners = pending.remove(key); }
                if (listeners != null) for (Consumer<String[]> callback : listeners) callback.accept(new String[]{"", ""});
            }
        }
        return () -> { cancelled.set(true); detach(key, listener); };
    }
    private synchronized void detach(String key, Consumer<String[]> listener) {
        List<Consumer<String[]>> listeners = pending.get(key);
        if (listeners != null) listeners.remove(listener);
    }
}
