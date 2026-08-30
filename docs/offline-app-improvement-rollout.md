# Plan de mejora de la app offline: cierre de la beta

Este documento registra el resultado del plan ejecutado sobre la rama
`codex/offline-app-improvements`. Todas las entregas se publicaron unicamente en
el canal beta y se probaron sobre un Fire TV Amazon AFTKRT conectado por ADB.

## Bloques entregados

| Bloque | Version beta | Resultado |
| --- | --- | --- |
| 1. Red y recuperacion | 451 | Reintentos y recuperacion de conectividad sin bucles agresivos. |
| 2. Smoke y metricas | 451 | Smoke ADB reproducible, informe JSON y puertas estrictas. |
| 3. Arquitectura de playback | 452 | Coordinacion de recuperacion extraida y cubierta por pruebas. |
| 4. Integracion del sistema | 453 | MediaSession, audio focus, noisy audio y PiP resiliente. |
| 5. EPG temporal | 454 | UTC, offsets por plataforma y transiciones DST normalizados. |
| 6. Sistema visual | 455 | Jerarquia, foco, contraste y tamaños coherentes para TV. |
| 7. Diagnostico guiado | 456 | Estado actual separado del historial y acciones por causa. |
| 8. Experiencia de dispositivo | 457 | Centro de dispositivo, handoff, capacidad y multiview seguro. |
| Hotfix de red | 458 | Failover inmediato ante fallos de transporte/IPv6 y recuperación visible sin pantalla negra. |
| Refactor incremental de grabaciones | 463 | Deteccion de conflictos extraida de la Activity y cubierta por pruebas puras. |
| 9. Deuda tecnica de diagnosticos | 470 | Clasificacion de errores y recomendaciones extraida de la Activity, sin dependencias de UI y con pruebas de precedencia. |
| 10. Restauracion de foco | 471 | Los paneles temporales restauran la ultima superficie enfocada visible en vez de elegir otra por orden interno. |
| 11. Puerta de reproduccion determinista | 472 | El smoke admite un canal estable y el arranque explicito evita que el fast-start cacheado lo sustituya. |
| 12. Formato de diagnostico desacoplado | 473 | Resolucion, codec, fps y bitrate salen de la Activity y quedan cubiertos por pruebas puras y deterministas. |
| Hotfix de carrera en arranque | 473 | El arranque diferido conserva el ID elegido aunque cambie la lista visible y no vuelve a preparar un canal que ya se esta reproduciendo. |
| 13. Recuperacion de catalogo desacoplada | 474 | La decision de refrescar tras errores 401/403/404 o token queda aislada, con cooldown y requisitos de acceso cubiertos por pruebas puras. |
| 14. Invalidacion de rutas aprendidas | 475 | Fallo, recuperacion por otra ruta, override temporal y modo manual se resuelven en una politica pura con pruebas de precedencia. |
| 15. Decision de autorreparacion | 476 | Reintento automatico, siguiente ruta y casos sin accion quedan aislados de la Activity y cubiertos contra bucles. |
| 16. Puerta de navegacion y foco | 476 | Un smoke ADB recorre Herramientas, TV/EPG y timeline, exige movimiento real del foco y retorno seguro al directo. |

## Validacion minima antes de promover

1. Instalar exactamente el APK beta candidato, sin recompilar entre la prueba y
   la publicacion.
2. Ejecutar la suite y lint:

   ```bash
   JAVA_HOME=/tmp/drbep-android-jbr ./gradlew --no-daemon --max-workers=2 test lint
   ```

3. Ejecutar el recorrido general en el Fire TV:

   ```bash
   STRICT_WARNINGS=1 CHECK_GUIDE_KEY=0 \
     PLAYBACK_CHANNEL_ID=<canal_estable> \
     REPORT_OUTPUT=/tmp/drbep-fire-smoke.json \
     scripts/offline_smoke_test.sh 192.168.93.189:5555
   ```

4. Ejecutar el recorrido especifico de dos reproductores. Esta puerta existe
   porque una colision de `MediaSession` en multiview no era observable en el
   smoke general:

   ```bash
   SCREENSHOT_OUTPUT=/tmp/drbep-multiview.png \
     LOG_OUTPUT=/tmp/drbep-multiview.log \
     scripts/offline_multiview_smoke_test.sh 192.168.93.189:5555
   ```

5. Ejecutar el recorrido de foco con mando en el Fire TV:

   ```bash
   scripts/offline_navigation_focus_smoke_test.sh 192.168.93.189:5555
   ```

6. Verificar que publicacion y APK probado tienen el mismo SHA-256 y certificado,
   y que el preflight y el canary del canal beta son correctos.

## Criterios de aceptacion

- La app conserva el proceso al pasar por el launcher y vuelve al primer plano.
- El canal principal alcanza primer frame sin error de fuente o DRM.
- Cuando se define `PLAYBACK_CHANNEL_ID`, el smoke no depende del canal adyacente y exige la señal del canal estable elegido.
- EPG y catalogo cargan sin rechazo ni reduccion inesperada.
- Multiview muestra la capacidad real, al menos dos canales distintos alcanzan
  primer frame y `READY`, y el canal principal se recupera al salir.
- No aparecen `FATAL EXCEPTION`, `PlaybackException`, colisiones de
  `MediaSession` ni nuevos errores de catalogo.

## Rollback

- Mantener el canal estable sin cambios mientras la beta esta en observacion.
- Si una beta falla, volver a publicar el APK beta anterior por su artefacto y
  SHA conocidos; no recompilar una version historica.
- Conservar el log, el informe JSON y la captura del dispositivo que justifican
  el rollback.

La promocion a estable queda fuera de este plan y requiere una decision expresa
despues del periodo de observacion de la beta 457.
