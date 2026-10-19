package com.drbep.tvplayer;
/** UI-thread mailbox: hidden surfaces retain only the newest presentation request. */
final class DeferredArchiveRefresh {
    private Runnable latest;
    void request(Runnable update) { latest = update; }
    boolean pending() { return latest != null; }
    void drain(boolean visible) {
        if (!visible || latest == null) return;
        Runnable update = latest;
        latest = null;
        update.run();
    }
}
