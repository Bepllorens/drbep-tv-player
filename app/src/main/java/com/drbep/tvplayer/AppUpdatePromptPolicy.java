package com.drbep.tvplayer;

final class AppUpdatePromptPolicy {
    static final long RETRY_INTERVAL_MS = 24L * 60L * 60L * 1000L;

    private AppUpdatePromptPolicy() {
    }

    static boolean shouldPrompt(
            int availableVersionCode,
            boolean required,
            int lastPromptedVersionCode,
            long lastPromptedAtMs,
            long nowMs
    ) {
        if (availableVersionCode <= 0) {
            return false;
        }
        if (required || lastPromptedVersionCode != availableVersionCode) {
            return true;
        }
        // Las versiones antiguas solo guardaban el codigo. Al no tener fecha,
        // dejamos que vuelvan a avisar en vez de silenciarlas para siempre.
        if (lastPromptedAtMs <= 0L || nowMs < lastPromptedAtMs) {
            return true;
        }
        return nowMs - lastPromptedAtMs >= RETRY_INTERVAL_MS;
    }
}
