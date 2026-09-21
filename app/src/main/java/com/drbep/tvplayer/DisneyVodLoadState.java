package com.drbep.tvplayer;

final class DisneyVodLoadState {
    private long generation;
    boolean catalogLoading;
    boolean catalogLoaded;

    long begin(boolean catalog) {
        catalogLoading = catalog;
        return ++generation;
    }

    boolean finish(long request, boolean catalog, boolean accepted) {
        if (request != generation) return false;
        if (catalog) {
            catalogLoading = false;
            catalogLoaded = accepted;
        }
        return true;
    }

    void reset() {
        generation++;
        catalogLoading = false;
        catalogLoaded = false;
    }
}
