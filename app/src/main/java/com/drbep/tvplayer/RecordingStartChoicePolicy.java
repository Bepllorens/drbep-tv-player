package com.drbep.tvplayer;

/** Time predicate only; channel support and permissions stay with the caller. */
final class RecordingStartChoicePolicy {
    private RecordingStartChoicePolicy() {}
    static boolean isInProgress(long start, long end, long now) {
        return start > 0L && start < now && end > now;
    }
}
