package com.drbep.tvplayer;

import java.util.function.Supplier;

/** Small generation gate; never holds the queue lock or performs SQL. */
final class MetadataPauseSignal {
    private volatile String pausedGeneration;
    synchronized String requestPause(Supplier<String> currentGeneration) {
        String generation=currentGeneration.get();
        pausedGeneration=generation;
        return generation;
    }
    synchronized void publishGeneration(Runnable persistGeneration) {
        persistGeneration.run();
        pausedGeneration=null;
    }
    boolean isPaused(String generation) {
        return generation.equals(pausedGeneration);
    }
}
