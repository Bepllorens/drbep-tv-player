package com.drbep.tvplayer;

final class PlaybackHealthClassifier {
    static final class Result {
        final String level;
        final String summary;
        final double rebufferRatio;

        Result(String level, String summary, double rebufferRatio) {
            this.level = level == null ? "unknown" : level;
            this.summary = summary == null ? "" : summary;
            this.rebufferRatio = Math.max(0d, rebufferRatio);
        }
    }

    private PlaybackHealthClassifier() {
    }

    static Result classify(PlayerController.PlaybackDiagnostics diagnostics, long sessionElapsedMs) {
        if (diagnostics == null) {
            return new Result("unknown", "Sin diagnostico de reproduccion", 0d);
        }
        String error = diagnostics.lastError == null ? "" : diagnostics.lastError.trim();
        if (!error.isEmpty()) {
            return new Result("error", "Error de reproduccion", rebufferRatio(diagnostics, sessionElapsedMs));
        }
        String state = diagnostics.playbackState == null ? "" : diagnostics.playbackState.trim().toUpperCase(java.util.Locale.ROOT);
        double ratio = rebufferRatio(diagnostics, sessionElapsedMs);
        if ("BUFFERING".equals(state)) {
            return new Result("loading", "Buffering", ratio);
        }
        if ("READY".equals(state) && !diagnostics.firstFrameRendered) {
            return new Result("warning", "Listo sin primer frame", ratio);
        }
        if (ratio >= 0.08d || diagnostics.bufferingCount >= 5) {
            return new Result("warning", "Buffer inestable", ratio);
        }
        if ("READY".equals(state)) {
            return new Result("ok", "Reproduccion estable", ratio);
        }
        if ("ENDED".equals(state)) {
            return new Result("ok", "Reproduccion finalizada", ratio);
        }
        if ("IDLE".equals(state)) {
            return new Result("loading", "Esperando reproduccion", ratio);
        }
        return new Result("unknown", state.isEmpty() ? "Estado desconocido" : state, ratio);
    }

    private static double rebufferRatio(PlayerController.PlaybackDiagnostics diagnostics, long sessionElapsedMs) {
        if (diagnostics == null || diagnostics.bufferingTotalMs <= 0L || sessionElapsedMs <= 0L) {
            return 0d;
        }
        double ratio = diagnostics.bufferingTotalMs / (double) sessionElapsedMs;
        return Math.round(ratio * 1000d) / 1000d;
    }
}
