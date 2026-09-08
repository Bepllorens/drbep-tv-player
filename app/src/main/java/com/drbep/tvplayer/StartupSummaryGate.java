package com.drbep.tvplayer;

/** UI-thread-owned lifetime of one home dialog's asynchronous summary. */
final class StartupSummaryGate {
    private long generation;
    private boolean open;

    long open() { open = true; return ++generation; }
    long current() { return generation; }
    boolean accepts(long token) { return open && token == generation; }
    boolean close(long token) {
        if (!accepts(token)) return false;
        open = false;
        return true;
    }
}
