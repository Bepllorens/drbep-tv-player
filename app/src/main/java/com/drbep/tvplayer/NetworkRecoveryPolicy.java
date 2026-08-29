package com.drbep.tvplayer;

final class NetworkRecoveryPolicy {
    private static final long[] RETRY_DELAYS_MS = {750L, 1_500L, 3_000L, 5_000L};

    private NetworkRecoveryPolicy() {
    }

    static long retryDelayMs(int attempt) {
        int index = Math.max(0, Math.min(RETRY_DELAYS_MS.length - 1, attempt));
        return RETRY_DELAYS_MS[index];
    }

    static boolean shouldRetryAfterRestore(boolean recoveryPending, boolean playbackActive, String phase) {
        if (!recoveryPending || playbackActive) {
            return false;
        }
        PlaybackSessionStateMachine.State state = PlaybackSessionStateMachine.stateForPhase(phase);
        return state == PlaybackSessionStateMachine.State.WAITING_NETWORK
                || state == PlaybackSessionStateMachine.State.FAILED
                || state == PlaybackSessionStateMachine.State.BUFFERING
                || state == PlaybackSessionStateMachine.State.RECOVERING;
    }
}
