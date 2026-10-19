package com.drbep.tvplayer;

/** Opt-in atrace markers, numeric identifiers only; no programme or session data. */
final class ArchivePageTrace {
    private static int sequence;
    private static String day = "";
    private static boolean pending;
    private static int publishedSequence;
    static synchronized void begin(String selectedDay) {
        day = selectedDay;
        pending = true;
        mark(++sequence, 0, "input");
    }
    static synchronized int capture(String selectedDay) {
        return pending && day.equals(selectedDay) ? sequence : 0;
    }
    static void mark(int request, int revision, String stage) {
        if (request == 0) return;
        android.os.Trace.beginSection("Archive.page." + request + "." + revision + "." + stage);
        android.os.Trace.endSection();
    }
    static void cost(String stage, long millis) {
        android.os.Trace.beginSection("Archive.cost." + stage + "." + millis);
        android.os.Trace.endSection();
    }
    static synchronized void published(int request,int revision) {
        mark(request,revision,"published");
        if(request==sequence)publishedSequence=request;
    }
    static synchronized void drawn(String selectedDay) {
        int request = capture(selectedDay);
        if (request == 0 || publishedSequence != request) return;
        mark(request, 0, "drawn");
        pending = false;
    }
}
