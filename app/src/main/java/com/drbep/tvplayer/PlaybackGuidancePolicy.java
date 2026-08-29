package com.drbep.tvplayer;

import java.util.Locale;

final class PlaybackGuidancePolicy {
    static final String ACTION_CLOSE = "close";
    static final String ACTION_RETRY = "retry";
    static final String ACTION_NEXT_ROUTE = "next_route";

    static final class Result {
        final String level;
        final String headline;
        final String explanation;
        final String nextStep;
        final String primaryAction;

        Result(String level, String headline, String explanation, String nextStep, String primaryAction) {
            this.level = safe(level, "warn");
            this.headline = safe(headline, "Estado sin determinar");
            this.explanation = safe(explanation, "No hay informacion suficiente para completar el diagnostico.");
            this.nextStep = safe(nextStep, "Reintenta la reproduccion y vuelve a abrir este panel.");
            this.primaryAction = safe(primaryAction, ACTION_RETRY);
        }
    }

    private PlaybackGuidancePolicy() {
    }

    static Result evaluate(PlayerController.PlaybackDiagnostics diagnostics, PlaybackDiagnosticsStore.ErrorRecord storedError) {
        if (diagnostics == null || (blank(diagnostics.channelName) && blank(diagnostics.targetUrl))) {
            if (storedError != null) {
                return fromError(storedError.message, true);
            }
            return new Result(
                    "warn",
                    "Aun no hay una reproduccion que analizar",
                    "El diagnostico necesita que se haya intentado abrir un canal.",
                    "Inicia un canal o pulsa Reintentar para generar una comprobacion nueva.",
                    ACTION_RETRY
            );
        }

        if (!diagnostics.networkAvailable) {
            return new Result(
                    "error",
                    "El dispositivo esta sin conexion",
                    "Android no detecta una red disponible para llegar al servidor o al proveedor.",
                    "Recupera la conexion Wi-Fi o Ethernet y despues pulsa Reintentar.",
                    ACTION_RETRY
            );
        }

        if (!blank(diagnostics.lastError)) {
            return fromError(diagnostics.lastError, false);
        }

        String state = upper(diagnostics.playbackState);
        String phase = lower(diagnostics.playbackPhase);
        if ("READY".equals(state) && diagnostics.firstFrameRendered) {
            if (diagnostics.bufferingCount >= 4 || diagnostics.bufferingTotalMs >= 6_000L || diagnostics.networkRecoveryAttempts >= 2) {
                return new Result(
                        "warn",
                        "La reproduccion funciona, pero es inestable",
                        "Se han detectado varias pausas de buffer o autorreparaciones durante esta sesion.",
                        "Prueba otra ruta. Si continua, revisa la calidad de la red del dispositivo.",
                        ACTION_NEXT_ROUTE
                );
            }
            if (!diagnostics.networkValidated) {
                return new Result(
                        "warn",
                        "La reproduccion funciona con red sin validar",
                        "Android ve la red, pero no confirma que tenga salida estable a Internet.",
                        "Puedes seguir viendo el canal; si aparecen cortes, revisa la red antes de cambiar la ruta.",
                        ACTION_CLOSE
                );
            }
            if (storedError != null) {
                return new Result(
                        "warn",
                        "El canal funciona ahora",
                        "Queda un fallo anterior guardado, pero no describe el estado de la reproduccion actual.",
                        "Si el canal sigue estable, limpia el ultimo fallo desde Acciones.",
                        ACTION_CLOSE
                );
            }
            if (diagnostics.usingFallback) {
                return new Result(
                        "ok",
                        "Reproduccion estable por ruta de compatibilidad",
                        "Hay video y la app ha elegido una ruta alternativa compatible con este dispositivo.",
                        "No necesitas hacer nada mientras imagen y sonido permanezcan estables.",
                        ACTION_CLOSE
                );
            }
            return new Result(
                    "ok",
                    "Reproduccion estable",
                    "Hay primer frame, la red esta disponible y no se detectan errores ni cortes relevantes.",
                    "No necesitas hacer nada.",
                    ACTION_CLOSE
            );
        }

        if ("READY".equals(state) && !diagnostics.firstFrameRendered) {
            return new Result(
                    "warn",
                    "El stream esta listo, pero no aparece imagen",
                    "La fuente ha arrancado, aunque el dispositivo todavia no ha renderizado el primer frame.",
                    "Espera unos segundos; si no aparece imagen, prueba otra ruta.",
                    ACTION_NEXT_ROUTE
            );
        }

        if ("BUFFERING".equals(state) || phase.contains("buffer") || phase.contains("waiting")) {
            return new Result(
                    "warn",
                    "La reproduccion esta esperando datos",
                    "El reproductor no recibe contenido con suficiente continuidad para avanzar.",
                    "Espera la autorreparacion. Si se repite, prueba otra ruta.",
                    ACTION_NEXT_ROUTE
            );
        }

        if ("ENDED".equals(state)) {
            return new Result(
                    "ok",
                    "La reproduccion ha finalizado",
                    "El reproductor recibio el final del contenido sin un error activo.",
                    "Reintenta solo si se trata de un canal que deberia seguir en directo.",
                    ACTION_RETRY
            );
        }

        return new Result(
                "warn",
                "La reproduccion todavia esta arrancando",
                "La sesion esta en estado " + safe(diagnostics.playbackState, "desconocido") + " y aun no hay video confirmado.",
                "Espera unos segundos. Si no progresa, pulsa Reintentar.",
                ACTION_RETRY
        );
    }

    private static Result fromError(String rawError, boolean historicalOnly) {
        String error = lower(rawError);
        String prefix = historicalOnly ? "El ultimo intento fallo: " : "La reproduccion ha fallado: ";
        if (containsAny(error, "401", "403", "token", "unauthorized", "forbidden", "expired", "session")) {
            return new Result(
                    "error",
                    prefix + "sesion o permisos",
                    "El servidor o el proveedor ha rechazado la autorizacion de esta reproduccion.",
                    "Actualiza el catalogo. Si persiste, reactiva el dispositivo o la cuenta del proveedor.",
                    ACTION_RETRY
            );
        }
        if (containsAny(error, "drm", "license", "licence", "widevine", "keys")) {
            return new Result(
                    "error",
                    prefix + "licencia DRM",
                    "No se ha podido obtener o usar la licencia necesaria para descifrar el contenido.",
                    "Prueba otra ruta; si persiste, actualiza el catalogo y la sesion del proveedor.",
                    ACTION_NEXT_ROUTE
            );
        }
        if (containsAny(error, "decoder", "codec", "format", "renderer")) {
            return new Result(
                    "error",
                    prefix + "formato no compatible",
                    "El dispositivo no ha podido decodificar el audio o el video recibido.",
                    "Prueba otra ruta para que la app use un formato compatible.",
                    ACTION_NEXT_ROUTE
            );
        }
        if (containsAny(error, "manifest", "playlist", "smoothstream", "dash", "hls", "parsing", "mime")) {
            return new Result(
                    "error",
                    prefix + "manifiesto del canal",
                    "La respuesta recibida no tiene el formato de stream que esperaba el reproductor.",
                    "Prueba otra ruta. Si todas fallan, actualiza el catalogo del proveedor.",
                    ACTION_NEXT_ROUTE
            );
        }
        if (containsAny(error, "timeout", "timed out", "network", "connection", "socket", "dns", "host")) {
            return new Result(
                    "error",
                    prefix + "conexion",
                    "La red no ha entregado los datos del canal a tiempo o ha cerrado la conexion.",
                    "Reintenta. Si vuelve a ocurrir, revisa la red y despues prueba otra ruta.",
                    ACTION_RETRY
            );
        }
        return new Result(
                "error",
                prefix + "ruta actual",
                "La ruta elegida no ha conseguido iniciar el contenido.",
                "Prueba otra ruta y conserva la que reproduzca imagen y sonido de forma estable.",
                ACTION_NEXT_ROUTE
        );
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }
}
