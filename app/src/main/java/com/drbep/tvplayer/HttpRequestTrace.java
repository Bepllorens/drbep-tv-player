package com.drbep.tvplayer;

import java.util.function.LongSupplier;

/** Numeric-only trace for one request; no URL, headers, body or Throwable accepted. */
final class HttpRequestTrace {
    interface Sink { void event(int phase, long operation, long a, long b, long c); }
    static final int START=0, HEADERS=1, END=2;
    static final HttpRequestTrace OFF = new HttpRequestTrace();
    private final long operation, started;
    private final LongSupplier clock;
    private final Sink sink;
    private long headersAt=-1;
    private boolean ended;
    private HttpRequestTrace(){operation=started=0;clock=null;sink=null;}
    HttpRequestTrace(long operation, LongSupplier clock, Sink sink) {
        this.operation=operation;this.clock=clock;this.sink=sink;started=clock.getAsLong();
        sink.event(START,operation,0,0,0);
    }
    synchronized void headers(int status) {
        if(sink==null || ended || headersAt>=0)return;
        headersAt=clock.getAsLong();
        sink.event(HEADERS,operation,Math.max(0,headersAt-started),status,0);
    }
    /** outcome: 0 response consumed, 1 cancelled, 2 exception. HTTP status is separate. */
    synchronized void finish(int outcome) {
        if(sink==null || ended)return;
        ended=true;
        long now=clock.getAsLong();
        sink.event(END,operation,Math.max(0,now-started),
                headersAt<0?-1:Math.max(0,now-headersAt),outcome);
    }
}
