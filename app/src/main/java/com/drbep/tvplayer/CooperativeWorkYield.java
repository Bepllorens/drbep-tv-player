package com.drbep.tvplayer;

/** Worker-only cooperative waits, with a cumulative budget to prevent starvation. */
final class CooperativeWorkYield {
    interface Clock { long now(); }
    interface Sleeper { void sleep(long ms) throws InterruptedException; }
    interface Check { void check() throws Exception; }
    interface Quiet { long remaining(); }
    interface Status { void waiting() throws Exception; void resumed() throws Exception; }
    private final Clock clock;
    private final Sleeper sleeper;
    private final Check check;
    private final Quiet quiet;
    private long budgetMs;
    CooperativeWorkYield(Clock clock, Sleeper sleeper, Check check, Quiet quiet, long budgetMs) {
        this.clock=clock;this.sleeper=sleeper;this.check=check;this.quiet=quiet;
        this.budgetMs=Math.max(0,budgetMs);
    }
    long yield(long maxWaitMs, Status status) throws Exception {
        check.check();
        long allowance=Math.min(Math.max(0,maxWaitMs),budgetMs);
        if (allowance==0 || quiet.remaining()<=0) return 0;
        long start=clock.now(), deadline=start+allowance;
        status.waiting();
        try {
            while (true) {
                check.check();
                long left=Math.min(deadline-clock.now(),quiet.remaining());
                if(left<=0)break;
                sleeper.sleep(Math.min(50,left));
            }
            check.check();
        } finally {
            long spent=Math.max(0,clock.now()-start);
            budgetMs=Math.max(0,budgetMs-spent);
        }
        status.resumed();
        return Math.max(0,clock.now()-start);
    }
}
