package com.drbep.tvplayer;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Shares only in-flight lookups. Keys must include authentication scope and artwork shape. */
final class ArtworkLookupQueue {
    private final Map<String, List<Consumer<String>>> pending = new HashMap<>();

    Runnable request(String key, Supplier<String> lookup, Consumer<String> ready,
                     Executor worker, Executor delivery) {
        AtomicBoolean cancelled = new AtomicBoolean();
        Consumer<String> listener = value -> delivery.execute(() -> {
            if (!cancelled.get()) ready.accept(value);
        });
        boolean launch;
        synchronized (this) {
            List<Consumer<String>> listeners = pending.get(key);
            launch = listeners == null;
            if (launch) {
                listeners = new ArrayList<>();
                pending.put(key, listeners);
            }
            listeners.add(listener);
        }
        if (launch) {
            try {
                worker.execute(() -> {
                    synchronized (this) {
                        List<Consumer<String>> listeners = pending.get(key);
                        if (listeners == null || listeners.isEmpty()) { pending.remove(key); return; }
                    }
                    String value;
                    try { value = lookup.get(); }
                    catch (Exception failed) { value = ""; }
                    finish(key, value == null ? "" : value);
                });
            } catch (RuntimeException rejected) { finish(key, ""); }
        }
        return () -> {
            cancelled.set(true);
            synchronized (this) {
                List<Consumer<String>> listeners = pending.get(key);
                if (listeners != null) listeners.remove(listener);
            }
        };
    }

    private void finish(String key, String value) {
        List<Consumer<String>> listeners;
        synchronized (this) { listeners = pending.remove(key); }
        if (listeners != null) for (Consumer<String> listener : listeners) {
            // One disposed/rejected delivery must not prevent another visible card from loading.
            try { listener.accept(value); } catch (RuntimeException ignored) { }
        }
    }
}
