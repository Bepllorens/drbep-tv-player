package com.drbep.tvplayer;

/** UI-thread queue: immediate first press, one trailing tune for a burst. */
final class RemoteZapQueue<T> {
    static final long QUIET_MS = 1000;
    static final long SETTLE_MS = 100;
    static final long MIN_SETTLE_MS = 40;
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
        // Dense queued input needs less settling than a held key's slower repeats.
        // Keep two intervals of margin, bounded by the tested 100 ms ceiling.
        long interval = immediate ? SETTLE_MS : now - lastPress;
        long settleMs = interval >= SETTLE_MS / 2
                ? SETTLE_MS : Math.min(SETTLE_MS, Math.max(MIN_SETTLE_MS, interval * 2 + 10));
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
