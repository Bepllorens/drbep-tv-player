package com.drbep.tvplayer;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/** Cancels only requests owned by a short-lived UI task, never a shared client. */
final class RequestCancellationScope implements AutoCloseable {
    private static final ThreadLocal<RequestCancellationScope> CURRENT = new ThreadLocal<>();
    private final Set<Runnable> pending = new HashSet<>();
    private boolean closed;

    interface Registration extends AutoCloseable { @Override void close(); }

    static Registration registerCurrent(Runnable cancel) throws InterruptedIOException {
        RequestCancellationScope scope = CURRENT.get();
        return scope == null ? () -> {} : scope.register(cancel);
    }

    private synchronized Registration register(Runnable cancel) throws InterruptedIOException {
        if (closed) {
            cancel.run();
            throw new InterruptedIOException("UI request cancelled");
        }
        pending.add(cancel);
        return () -> { synchronized (this) { pending.remove(cancel); } };
    }

    synchronized boolean isClosed() { return closed; }
    synchronized int activeRequestCount() { return pending.size(); }

    <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            if (isClosed()) throw new InterruptedIOException("UI task cancelled");
            RequestCancellationScope previous = CURRENT.get();
            CURRENT.set(this);
            try { return task.call(); }
            finally {
                if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
            }
        };
    }

    Runnable wrapRunnable(Runnable task) {
        return () -> {
            if (isClosed()) return;
            RequestCancellationScope previous = CURRENT.get();
            CURRENT.set(this);
            try { task.run(); }
            finally {
                if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
            }
        };
    }

    @Override public void close() {
        ArrayList<Runnable> cancellations;
        synchronized (this) {
            if (closed) return;
            closed = true;
            cancellations = new ArrayList<>(pending);
            pending.clear();
        }
        for (Runnable cancel : cancellations) cancel.run();
    }
}
