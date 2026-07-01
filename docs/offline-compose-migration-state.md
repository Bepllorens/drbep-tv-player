# Offline App Compose Migration State

Fecha: 2026-06-30

## Version actual

- Canal beta publicado: `2.0.176-beta-live-ready-overlay`
- `versionCode`: `176`
- APK beta: `https://iptv.bepllorens.com/api/offline/app/apk?channel=beta`
- Dispositivo de prueba principal: Fire Stick `192.168.93.16:5555`

## Estado funcional validado

- HUD inferior y overlay lateral ya estan operativos tras los ajustes de tamano, foco y scroll.
- La lista lateral de canales desplaza correctamente manteniendo visible la fila enfocada.
- El picker de canal en multiview muestra cursor y permite seleccionar.
- `La 1 UHD` reproduce usando manifest DASH local parcheado para ClearKey/2160p.
- El panel de grabaciones navega con mando entre `Completadas`, `Programadas` y `Refrescar`.
- `Programadas` ya filtra registros cerrados o vencidos y `Refrescar` fuerza lectura no-cache del backend.
- Movistar ISM y Orange ya no muestran el aviso superior `VOD listo` al zapear canales live directos.

## Migracion Compose completada o avanzada

- Infra Compose habilitada en Gradle con Kotlin/Compose.
- HUD de zapping, timeshift, badges, busqueda rapida y controles tactiles migrados a superficies Compose.
- Overlay lateral migrado en cabecera, controles, buscador y lista de canales.
- Menus TV/herramientas/ajustes y paneles de mensaje/input migrados a paneles Compose.
- Global search, mini guia, resultados EPG, listas personales y canales rapidos migrados.
- Guia timeline: cabecera, escala, detalle y filas/bloques visuales en Compose.
- EPG visual y VOD visual/detail/action panels migrados.
- Multiview conserva reproductores nativos, con header y overlays de tile en Compose.
- Grabaciones ahora usan una unica superficie Compose para cabecera, detalle y lista.
- Diagnostico de playback e historial de fallos usan un panel Compose estructurado con secciones, tonos de ruta/error y acciones con foco inicial.
- El aviso de nueva version reutiliza el panel estructurado Compose para mostrar version actual, objetivo, canal, APK y changelog de forma legible.
- El diagnostico de actualizacion y la pantalla post-update tambien usan el panel estructurado Compose con ultimo intento, preflight, versiones y changelog.
- Los resumentes de ajustes/sistema/catalogo/familia pasan automaticamente a filas Compose cuando el texto viene en formato `Clave: valor`, conservando fallback de texto libre.

## Ultimos arreglos importantes

- `RemoteInputRouter` gestiona foco superior de grabaciones con izquierda/derecha y evita que izquierda cierre el panel.
- `RecordingsRepository` anade cache-buster y cabeceras no-cache para grabaciones.
- `RecordingsRepository` filtra programadas vivas por estado y descarta programaciones con `end_time` vencido.
- `HttpClient` desactiva cache de `HttpURLConnection`.
- Se elimino XML legacy de varios dialogos/listas ya reemplazados por Compose.

## Pendiente sugerido al retomar

- Revisar restos de XML/layouts legacy que aun esten vivos y decidir si migrarlos o dejarlos como contenedores nativos.
- Probar en Fire Stick y tablet el nuevo panel de diagnostico playback, especialmente foco de botones y lectura de URLs largas.
- Afinar UX de grabaciones si queremos filtros tipo dashboard (`Grabando`, `Fallidas`, `Completadas`, etc.) en vez de solo `Completadas/Programadas`.
- Probar v174 en telefono Android y Fire Stick real con actualizacion desde app, no solo instalacion ADB.
- Revisar warnings conocidos de Glide/imagenes para reducir ruido en logs, aunque no son crash.

## Verificacion reciente

- `./gradlew :app:testDebugUnitTest :app:assembleDebug` OK.
- `scripts/publish_offline_update.sh --channel beta` OK para v174.
- Instalacion ADB en `.16` OK.
- Arranque en `.16` sin `FATAL EXCEPTION`.
