# Orden de listas y retorno de Palantir — 2026-11-03

## Estado
Desarrollo en .185. Sin desplegar backend, instalar esta candidata, publicar ni cambiar stable.
Los cinco archivos del ensayo Tivify de otra IA se conservan; no forman parte de la candidata aislada.

## Orden
Lectura de producción: ZapiTV (plataforma5,232 canales activos) y Vodafone (plataforma6,131) tienen sort_order=0.
El catálogo ordenaba esos empates por nombre. Los canales importados llevan external_id manual_ y conservan el orden de inserción.
offline_channel_order.go mantiene los órdenes personalizados y explícitos; para importados sin dial conserva el orden de creación.
No se inventan diales ni se actualiza la base. Los importadores siguen leyendo tvg-chno cuando existe.
Pruebas específicas y todo internal/api pasan (221,601s). Pendiente preparar despliegue respaldado y autorización antes de activar.

## Palantir
MainActivity.java y PalantirBrowser.kt añaden Volver al vídeo con ActivePlaybackReturnPolicy.
Requiere permiso Palantir, misma identidad y reproducción activa. Cierra modal y devuelve foco, sin iniciar una nueva resolución.
13 pruebas de política, reproducción, foco y permisos pasan; release perfilable ARM32 beta786 compila.
Worktree aislado: /srv/drbep-dev/worktrees/android-order-palantir-20261103
Candidata privada: /srv/drbep-dev/reviews/android-order-palantir-20261103/palantir-return-786.apk
SHA256 bb682e398c9e269c888149c28dd6b51a9d9da96e019680f57de96a05871c2643
Firma de pruebas coincide con Fire. NO instalada todavía. Pendiente navegación real y retorno conservando posición.

## Audio
Usuario identifica La bola negra, versión4K, proveedor1fichier. Prueba18:07–18:08:
AudioTimeline EAC3,48000Hz,6 canales; encodedPtsVsSamplesUs=0 en muestras.
AudioFlinger confirma salida directa HDMI EAC3; no prueba por sí solo la causa del desfase.
Backend PalantirStream retransmite bytes sin transformar audio.
El usuario confirma que ahora va bien y pide dejar el audio como está. NO se aplica cambio PCM ni retraso ni modificación global.
No se atribuye el fallo a memoria sin evidencia. Diagnósticos privados (sin medios) en la carpeta de revisión; no añadirlos a Git.

## Tivify
Continúa pendiente. El bloqueo de revisión automática sobre el cambio CBCS sigue vigente.
La comparación actual muestra una excepción ajena no confirmada para La1 hacia /decrypt.
La ruta original precede los últimos cambios del dashboard; no hay prueba histórica suficiente para atribuir o descartar todas las regresiones.
Ver documentos tivify-fire-diagnostico-20261103.md y tivify-live-corruption-diagnostico-20261103.md.
