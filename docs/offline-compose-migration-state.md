# Offline App Compose Migration State

Fecha: 2026-07-17

## Version actual

- Canal beta publicado: `2.0.319-beta-fast-startup-timeline`
- `versionCode`: `319`
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
- El arranque en `.16` reutiliza cache parseada del catalogo: ultimo arranque medido `startup catalog loaded ... durationMs=1045`.
- La EPG progresiva carga plataformas completas en segundo plano con bloques locales rapidos, sin fallback remoto canal a canal.
- La guia Timeline abre desde snapshot local dirigido y ya no deberia parpadear abriendo primero una vista rapida de dos programas.
- Movistar ISM, Tivify, PlutoTV, Runtime y otras plataformas completan lotes EPG en diferido con tiempos de cientos de ms por bloque.

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

- `CatalogSnapshotStore` deja de invalidar la cache parseada por tener mas de 5000 items, porque el catalogo total incluye VOD y no implica corrupcion.
- `EpgRepository` separa la lectura dirigida de EPG del limite global de tamano del snapshot, permitiendo timeline/progresiva desde snapshot local grande.
- `MainActivity` abre la guia Timeline enriquecida una sola vez cuando los datos reales estan listos, evitando cerrar/reabrir el panel.
- La carga progresiva de EPG usa snapshot local dirigido y reduce el intervalo entre bloques para hidratar el catalogo en pocos minutos sin bloquear zapping.
- El backend EPG resuelve referencias directas por `tvg_id`/nombre cuando el canal numerico del catalogo offline no coincide.
- `RemoteInputRouter` gestiona foco superior de grabaciones con izquierda/derecha y evita que izquierda cierre el panel.
- `RecordingsRepository` anade cache-buster y cabeceras no-cache para grabaciones.
- `RecordingsRepository` filtra programadas vivas por estado y descarta programaciones con `end_time` vencido.
- `HttpClient` desactiva cache de `HttpURLConnection`.
- Se elimino XML legacy de varios dialogos/listas ya reemplazados por Compose.
- Orange fuerza la pista de video soportada de mayor calidad y usa buffers mas cortos para acelerar el zapping.
- El player deja de mostrar avisos superiores durante `buffering`, `ready`, Widevine y rutas directas para evitar ruido visual.
- Los filtros internos `all`/`Todos` se ocultan en navegacion cuando existen filtros visibles de plataforma o grupo.
- Los paneles VOD/EPG/programa conservan mejor el retorno al panel anterior al cerrar o al pasar por control parental.
- Orange vuelve a preparar una sola ruta resuelta desde el inicio y evita el override manual tardio de pista maxima, reduciendo riesgo de pantalla negra durante el arranque.
- Los logs de playback incluyen tiempos hasta `READY` y primer frame para diagnosticar zapping lento.

## Pendiente sugerido al retomar

- Revisar restos de XML/layouts legacy que aun esten vivos y decidir si migrarlos o dejarlos como contenedores nativos.
- Probar `2.0.319-beta-fast-startup-timeline` desde el actualizador de la app, no solo por ADB.
- Confirmar visualmente en Fire Stick que el boton `Guia` ya no parpadea ni reabre el Timeline.
- Revisar si conviene separar VOD pesado del catalogo de arranque para bajar aun mas memoria/tiempo en primer parseo tras cambio de snapshot.
- Probar en Fire Stick y tablet el panel de diagnostico playback, especialmente foco de botones y lectura de URLs largas.
- Afinar UX de grabaciones si queremos filtros tipo dashboard (`Grabando`, `Fallidas`, `Completadas`, etc.) en vez de solo `Completadas/Programadas`.
- Revisar warnings conocidos de Glide/imagenes para reducir ruido en logs, aunque no son crash.

## Verificacion reciente

- `./gradlew :app:testDebugUnitTest :app:assembleDebug` OK en la tanda Compose previa.
- `./gradlew :app:compileReleaseJavaWithJavac` OK para v319.
- `scripts/publish_offline_update.sh --channel beta` OK para v319.
- Instalacion ADB en `.16` OK para v319.
- Segundo arranque medido en `.16`: cache parseada hit, catalogo listo en `1045 ms`.
- Arranque en `.16` sin `FATAL EXCEPTION`.
