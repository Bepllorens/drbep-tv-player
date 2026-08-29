package com.drbep.tvplayer;

final class PlaybackNetworkRecoveryCoordinator {
    enum Action {
        NONE,
        WAIT_FOR_NETWORK,
        MARK_PLAYING,
        SCHEDULE_RECOVERY
    }

    static final class Outcome {
        final Action action;
        final long delayMs;
        final int attempt;

        private Outcome(Action action, long delayMs, int attempt) {
            this.action = action == null ? Action.NONE : action;
            this.delayMs = Math.max(0L, delayMs);
            this.attempt = Math.max(0, attempt);
        }

        static Outcome none() {
            return new Outcome(Action.NONE, 0L, 0);
        }
    }

    static final class Snapshot {
        final boolean available;
        final boolean validated;
        final String transport;
        final boolean recoveryPending;
        final int recoveryAttempts;

        Snapshot(boolean available, boolean validated, String transport, boolean recoveryPending, int recoveryAttempts) {
            this.available = available;
            this.validated = validated;
            this.transport = safeTransport(transport);
            this.recoveryPending = recoveryPending;
            this.recoveryAttempts = Math.max(0, recoveryAttempts);
        }
    }

    private boolean available = true;
    private boolean validated;
    private String transport = "desconocida";
    private boolean recoveryPending;
    private int recoveryAttempts;

    Outcome updateNetworkState(
            boolean nextAvailable,
            boolean nextValidated,
            String nextTransport,
            boolean hasCurrentRequest,
            boolean playbackEnded,
            boolean playbackActive,
            String playbackPhase
    ) {
        boolean restored = !available && nextAvailable;
        available = nextAvailable;
        validated = nextValidated;
        transport = safeTransport(nextTransport);

        if (!available) {
            if (hasCurrentRequest && !playbackEnded) {
                recoveryPending = true;
                return new Outcome(Action.WAIT_FOR_NETWORK, 0L, recoveryAttempts);
            }
            return Outcome.none();
        }

        if (!restored || !recoveryPending) {
            return Outcome.none();
        }
        if (!NetworkRecoveryPolicy.shouldRetryAfterRestore(recoveryPending, playbackActive, playbackPhase)) {
            if (playbackActive) {
                recoveryPending = false;
                recoveryAttempts = 0;
                return new Outcome(Action.MARK_PLAYING, 0L, 0);
            }
            return Outcome.none();
        }
        return new Outcome(
                Action.SCHEDULE_RECOVERY,
                NetworkRecoveryPolicy.retryDelayMs(recoveryAttempts),
                recoveryAttempts
        );
    }

    boolean deferUntilRestored(boolean hasCurrentRequest) {
        if (available || !hasCurrentRequest) {
            return false;
        }
        recoveryPending = true;
        return true;
    }

    boolean beginScheduledRecovery(boolean requestIsCurrent) {
        if (!available || !recoveryPending || !requestIsCurrent) {
            return false;
        }
        recoveryPending = false;
        recoveryAttempts++;
        return true;
    }

    void onFirstFrame() {
        recoveryPending = false;
    }

    void resetAttemptsForChannelChange(boolean channelChanged) {
        if (channelChanged) {
            recoveryAttempts = 0;
        }
    }

    Snapshot snapshot() {
        return new Snapshot(available, validated, transport, recoveryPending, recoveryAttempts);
    }

    private static String safeTransport(String value) {
        String clean = value == null ? "" : value.trim();
        return clean.isEmpty() ? "desconocida" : clean;
    }
}
