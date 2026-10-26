package com.drbep.tvplayer;

import java.util.*;
import java.util.function.LongSupplier;

/** Short-lived observers; sequence numbers protect against reordered renew/release requests. */
final class DiagnosticDetailLease {
    static final long LEASE_MS=20_000L;
    private static final long TOMBSTONE_MS=120_000L;
    private static final int LIMIT=16;
    private static final class Observer {
        long sequence,until,retainUntil;
        boolean active;
    }
    private final Map<String,Observer> observers=new HashMap<>();
    private final LongSupplier clock;
    DiagnosticDetailLease(){this(()->System.nanoTime()/1_000_000L);}
    DiagnosticDetailLease(LongSupplier clock){this.clock=clock;}
    synchronized boolean update(String client,long sequence,boolean active) {
        if(client==null || !client.matches("[A-Za-z0-9_-]{1,64}") || sequence<1)throw new IllegalArgumentException();
        long now=clock.getAsLong();
        observers.entrySet().removeIf(e->e.getValue().retainUntil<=now);
        Observer value=observers.get(client);
        if(value!=null && sequence<=value.sequence)return true;
        if(value==null){
            if(observers.size()>=LIMIT)return false;
            value=new Observer();observers.put(client,value);
        }
        value.sequence=sequence;value.active=active;value.until=active?now+LEASE_MS:now;
        value.retainUntil=now+TOMBSTONE_MS;
        return true;
    }
    synchronized int activeCount() {
        long now=clock.getAsLong();int count=0;
        for(Observer value:observers.values())if(value.active && value.until>now)count++;
        return count;
    }
    synchronized void clear(){observers.clear();}
}
