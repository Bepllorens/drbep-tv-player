package com.drbep.tvplayer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded, request-driven refresh state. No timer and no work for a hidden page. */
final class PanelGuideRefreshGate {
    static final long FRESH_MS = 60_000, RETRY_MS = 5_000;
    static final int MAX_ACTIVE = 2, MAX_KEYS = 32;
    private static final class Entry {
        boolean running = true;
        long expires;
    }
    static final class Ticket {
        final String key;
        private final Entry entry;
        private Ticket(String key, Entry entry) { this.key=key; this.entry=entry; }
    }
    private final LinkedHashMap<String,Entry> entries = new LinkedHashMap<>(16,0.75f,true);
    private int active;

    synchronized Ticket begin(String key, long now) {
        Entry old=entries.get(key);
        if (old!=null && (old.running || now<old.expires)) return null;
        if (active>=MAX_ACTIVE) return null;
        if (!entries.containsKey(key) && entries.size()>=MAX_KEYS) {
            Iterator<Map.Entry<String,Entry>> it=entries.entrySet().iterator();
            while(it.hasNext()) if(!it.next().getValue().running){it.remove();break;}
        }
        if (!entries.containsKey(key) && entries.size()>=MAX_KEYS) return null;
        Entry next=new Entry();entries.put(key,next);active++;
        return new Ticket(key,next);
    }
    synchronized boolean current(Ticket ticket) {
        return entries.get(ticket.key)==ticket.entry && ticket.entry.running;
    }
    synchronized void complete(Ticket ticket, long now, boolean success) {
        if (!current(ticket)) return;
        ticket.entry.running=false;ticket.entry.expires=now+(success?FRESH_MS:RETRY_MS);active--;
    }
    synchronized boolean busy(){return active>0;}
    synchronized int size(){return entries.size();}
    synchronized void clear(){entries.clear();active=0;}
}
