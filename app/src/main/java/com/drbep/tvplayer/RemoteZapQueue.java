package com.drbep.tvplayer;

/** UI-thread queue: immediate first press, one trailing tune for a burst. */
final class RemoteZapQueue<T> {
    static final long QUIET_MS = 1000;
    static final long SETTLE_MS = 100;
    static final long MIN_SETTLE_MS = 30;
    interface Scheduler {
        void post(Runnable task, long delayMs);
        void remove(Runnable task);
    }
    interface Host<T> {
        int size();
        int currentIndex();
        T item(int index);
        void tune(int index);
    }
    private final Scheduler scheduler;
    private final Host<T> host;
    private long lastPress = Long.MIN_VALUE;
    private int pendingIndex = -1, pendingSize, pendingAnchor;
    private T pendingItem;
    private Runnable scheduled;

    RemoteZapQueue(Scheduler scheduler, Host<T> host) {
        this.scheduler = scheduler; this.host = host;
    }
    void cancel() {
        if (scheduled != null) scheduler.remove(scheduled);
        scheduled = null; pendingIndex = -1; pendingItem = null;
        lastPress = Long.MIN_VALUE;
    }
    // A real held-key release tells us that no more repeat events are coming.
    // Commit immediately instead of waiting for the trailing timer.
    void finishBurst() {
        Runnable finish = scheduled;
        if (finish != null) {
            scheduler.remove(finish);
            finish.run();
        }
        lastPress = Long.MIN_VALUE;
    }
    private boolean validPending() {
        return pendingIndex >= 0 && host.size() == pendingSize
                && host.currentIndex() == pendingAnchor
                && host.item(pendingIndex) == pendingItem;
    }
    void press(int delta, long now) {
        if (pendingIndex >= 0 && !validPending()) cancel();
        int count = host.size();
        if (count <= 0) { cancel(); return; }
        int anchor = pendingIndex >= 0 ? pendingIndex : Math.max(0, host.currentIndex());
        int next = (int)((((long)anchor + delta) % count + count) % count);
        boolean immediate = lastPress == Long.MIN_VALUE || now < lastPress || now - lastPress >= QUIET_MS;
        // One repeat interval plus a small jitter margin is enough for a trailing tune.
        // Keep the 100 ms ceiling for slower repeats and the immediate isolated press.
        long interval = immediate ? SETTLE_MS : now - lastPress;
        long settleMs = Math.min(SETTLE_MS, Math.max(MIN_SETTLE_MS, interval + 15));
        lastPress = now;
        if (scheduled != null) scheduler.remove(scheduled);
        scheduled = null;
        if (immediate) {
            pendingIndex = -1; pendingItem = null;
            host.tune(next);
            return;
        }
        pendingIndex = next; pendingItem = host.item(next);
        pendingSize = count; pendingAnchor = host.currentIndex();
        Runnable task = new Runnable() {
            @Override public void run() {
                if (scheduled != this) return;
                boolean valid = validPending();
                int target = pendingIndex;
                scheduled = null; pendingIndex = -1; pendingItem = null;
                if (valid) host.tune(target);
                else cancel();
            }
        };
        scheduled = task;
        scheduler.post(task, settleMs);
    }
}
