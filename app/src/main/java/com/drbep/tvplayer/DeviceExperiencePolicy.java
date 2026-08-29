package com.drbep.tvplayer;

final class DeviceExperiencePolicy {
    static final class Result {
        final String level;
        final String headline;
        final String remoteSummary;
        final String catalogSummary;
        final String capacitySummary;
        final boolean handoffReady;

        Result(String level, String headline, String remoteSummary, String catalogSummary, String capacitySummary, boolean handoffReady) {
            this.level = safe(level, "warn");
            this.headline = safe(headline, "Estado sin determinar");
            this.remoteSummary = safe(remoteSummary, "Control remoto sin determinar");
            this.catalogSummary = safe(catalogSummary, "Catalogo sin determinar");
            this.capacitySummary = safe(capacitySummary, "Capacidad sin determinar");
            this.handoffReady = handoffReady;
        }
    }

    private DeviceExperiencePolicy() {
    }

    static Result evaluate(
            boolean standaloneMode,
            boolean activated,
            boolean catalogAvailable,
            boolean catalogExpired,
            boolean realtimeCommandsRunning,
            int channelCount,
            int vodCount,
            int maxMultiViewStreams,
            boolean lowRam
    ) {
        int safeChannels = Math.max(0, channelCount);
        int safeVod = Math.max(0, vodCount);
        int safeStreams = Math.max(1, maxMultiViewStreams);
        String capacity = safeStreams + " reproducciones simultaneas"
                + (lowRam ? " · modo memoria reducida" : "");

        if (standaloneMode && !activated) {
            return new Result(
                    "error",
                    "Este dispositivo necesita activacion",
                    "No puede recibir contenido remoto hasta completar la activacion.",
                    "Catalogo local no vinculado a un usuario.",
                    capacity,
                    false
            );
        }
        if (!catalogAvailable || safeChannels <= 0) {
            return new Result(
                    "error",
                    "El catalogo no esta disponible",
                    realtimeCommandsRunning ? "Control remoto conectado." : "Control remoto en modo heartbeat.",
                    "Actualiza o repara el catalogo antes de reproducir.",
                    capacity,
                    false
            );
        }
        if (catalogExpired) {
            return new Result(
                    "warn",
                    "Listo con catalogo caducado",
                    realtimeCommandsRunning ? "Puede recibir contenido remoto en tiempo real." : "Puede recibir ordenes mediante heartbeat.",
                    safeChannels + " canales · " + safeVod + " titulos · conviene actualizar",
                    capacity,
                    realtimeCommandsRunning
            );
        }
        if (!realtimeCommandsRunning && standaloneMode) {
            return new Result(
                    "warn",
                    "Operativo con control remoto diferido",
                    "Las ordenes se reciben por heartbeat; pueden tardar unos segundos.",
                    safeChannels + " canales · " + safeVod + " titulos disponibles",
                    capacity,
                    true
            );
        }
        return new Result(
                "ok",
                "Listo para recibir y continuar contenido",
                standaloneMode
                        ? "Control remoto en tiempo real: directo, VOD, pausa, reanudacion y parada."
                        : "Control local activo.",
                safeChannels + " canales · " + safeVod + " titulos disponibles",
                capacity,
                standaloneMode
        );
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
