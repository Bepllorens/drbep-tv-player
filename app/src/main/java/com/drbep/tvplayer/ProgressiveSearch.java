package com.drbep.tvplayer;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Bounded independent sources. Each request owns its results, deadlines and cancellation. */
final class ProgressiveSearch<T> implements AutoCloseable {
    enum State { LOADING, READY, FAILED, TIMED_OUT, COOLDOWN }
    static final class Source<T> {
        final String id;
        final long timeoutMs;
        final boolean network;
        final Callable<List<T>> query;
        Source(String id, long timeoutMs, boolean network, Callable<List<T>> query) {
            this.id = id;
            this.timeoutMs = Math.max(1L, timeoutMs);
            this.network = network;
            this.query = query;
        }
    }
    static final class Part<T> {
        final String id;
        final State state;
        final List<T> rows;
        Part(String id, State state, List<T> rows) {
            this.id = id; this.state = state;
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }
    }
    static final class Snapshot<T> {
        final List<Part<T>> parts;
        Snapshot(List<Part<T>> parts) {
            this.parts = Collections.unmodifiableList(parts);
        }
        boolean complete() {
            for (Part<T> part : parts) if (part.state == State.LOADING) return false;
            return true;
        }
    }
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(
            3, 3, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(12),
            task -> { Thread t = new Thread(task, "drbep-search"); t.setDaemon(true); return t; });
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1,
            task -> { Thread t = new Thread(task, "drbep-search-deadline"); t.setDaemon(true); return t; });
    private final Map<String, Long> unreachableUntil = new HashMap<>();
    private static final long COOLDOWN_NS = TimeUnit.SECONDS.toNanos(30L);
    { deadlines.setRemoveOnCancelPolicy(true); }
    private Request current;
    private boolean closed;

    private final class Slot {
        final Source<T> source;
        State state = State.LOADING;
        List<T> rows = Collections.emptyList();
        Future<?> work, deadline;
        Slot(Source<T> source) { this.source = source; }
    }
    private final class Request {
        final List<Slot> slots = new ArrayList<>();
        final Consumer<Snapshot<T>> listener;
        Request(List<Source<T>> sources, Consumer<Snapshot<T>> listener) {
            this.listener = listener;
            for (Source<T> source : sources) slots.add(new Slot(source));
        }
    }

    synchronized void start(List<Source<T>> sources, boolean retry,
                            Consumer<Snapshot<T>> listener) {
        cancel();
        if (closed) return;
        Request request = new Request(sources, listener);
        current = request;
        for (Slot slot : request.slots) {
            Long until = unreachableUntil.get(slot.source.id);
            if (!retry && slot.source.network && until != null && System.nanoTime() < until) {
                slot.state = State.COOLDOWN;
            }
        }
        publish(request);
        for (Slot slot : request.slots) {
            if (current != request || closed) break;
            if (slot.state != State.LOADING) continue;
            try {
                slot.work = workers.submit(() -> {
                    try {
                        List<T> rows = slot.source.query.call();
                        finish(request, slot, rows == null ? Collections.emptyList() : rows, null, false);
                    } catch (Exception error) {
                        finish(request, slot, Collections.emptyList(), error, false);
                    }
                });
                slot.deadline = deadlines.schedule(
                        () -> finish(request, slot, Collections.emptyList(), null, true),
                        slot.source.timeoutMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException error) {
                finish(request, slot, Collections.emptyList(), error, false);
            }
        }
    }

    private synchronized void finish(Request request, Slot slot, List<T> rows,
                                     Exception error, boolean timeout) {
        if (closed || current != request || slot.state != State.LOADING) return;
        slot.state = timeout ? State.TIMED_OUT : error == null ? State.READY : State.FAILED;
        slot.rows = new ArrayList<>(rows);
        if (slot.deadline != null) slot.deadline.cancel(false);
        if (timeout && slot.work != null) slot.work.cancel(true);
        if (slot.source.network) {
            if (error == null && !timeout) unreachableUntil.remove(slot.source.id);
            else if (timeout || connectionFailure(error))
                unreachableUntil.put(slot.source.id, System.nanoTime() + COOLDOWN_NS);
        }
        publish(request);
    }
    private void publish(Request request) {
        List<Part<T>> parts = new ArrayList<>();
        for (Slot slot : request.slots) parts.add(new Part<>(slot.source.id, slot.state, slot.rows));
        // Serialized notifications preserve order when the UI posts them to its event loop.
        request.listener.accept(new Snapshot<>(parts));
    }
    private static boolean connectionFailure(Throwable error) {
        for (int depth = 0; error != null && depth < 6; depth++, error = error.getCause()) {
            if (error instanceof ConnectException || error instanceof UnknownHostException
                    || error instanceof SocketTimeoutException || error instanceof NoRouteToHostException)
                return true;
        }
        return false;
    }
    synchronized void cancel() {
        Request request = current;
        current = null;
        if (request != null) for (Slot slot : request.slots) {
            if (slot.work != null) slot.work.cancel(true);
            if (slot.deadline != null) slot.deadline.cancel(false);
        }
        workers.purge();
    }
    @Override public synchronized void close() {
        closed = true;
        cancel();
        workers.shutdownNow();
        deadlines.shutdownNow();
        unreachableUntil.clear();
    }
}
