# DRBEP Offline App Current State

Fecha: 2026-07-17

## Version de referencia

- Canal beta publicado: `2.0.320-beta-vod-hud-smoke-polish`
- `versionCode`: `320`
- APK beta: `https://iptv.bepllorens.com/api/offline/app/apk?channel=beta`
- Dispositivo principal de prueba: Fire Stick `192.168.93.16:5555`
- Rama app: `codex/offline-firestick-app`
- Backend relacionado: `codex/offline-user-snapshots`

## Estado general

La app offline ya es operativa en Fire Stick, tablet y movil Android, con UI mayoritariamente migrada a Compose y catalogo offline seguro basado en snapshot firmado, permisos por usuario y token de activacion. La experiencia principal se centra en TV live, zapping, EPG, VOD, grabaciones, control parental, actualizaciones beta/stable y diagnostico remoto desde dashboard.

## Validado recientemente

- Arranque rapido en `.16`: segundo arranque medido con cache parseada, `startup catalog loaded ... durationMs=1045`.
- El boton `Guia` abre correctamente el Timeline sin el parpadeo de abrir una vista rapida y reabrir con datos completos.
- La EPG progresiva carga plataformas completas en segundo plano desde snapshot local dirigido.
- Movistar ISM, Tivify, PlutoTV, Runtime, Adultes e Izzi Go completan lotes EPG con tiempos de cientos de ms por bloque.
- HUD inferior, overlay lateral, foco y scroll de lista de canales funcionan en Fire TV.
- Grabaciones navega con izquierda/derecha entre `Completadas`, `Programadas` y `Refrescar`.
- Programadas filtra registros antiguos o vencidos y `Refrescar` fuerza lectura no-cache del backend.
- Multiview permite abrir lista de canales con cursor y cambiar tile.
- `La 1 UHD` funciona con el manifest DASH/ClearKey adecuado cuando el backend exporta la ruta correcta.
- Movistar ISM y Orange ya no muestran avisos superiores ruidosos tipo `VOD listo` al zapear canales live.

## Compose y UX

- Infra Compose activa en Gradle con Kotlin/Compose.
- HUD de zapping, timeshift, badges, busqueda rapida y controles tactiles estan migrados a Compose.
- Overlay lateral esta migrado en cabecera, controles, buscador y lista de canales.
- Menus TV/EPG, herramientas, ajustes, mensajes e inputs usan paneles Compose.
- Busqueda global, mini guia, resultados EPG, listas personales y canales rapidos usan paneles Compose fullscreen.
- Guia Timeline usa Compose para cabecera, escala, detalle, filas y bloques visuales.
- EPG visual, VOD visual, ficha VOD y acciones VOD estan migrados.
- Multiview conserva reproductores nativos, con header y overlays de tile en Compose.
- Grabaciones usa una unica superficie Compose para cabecera, detalle y lista.
- Diagnostico de playback, historial de fallos, aviso de actualizacion y post-update usan paneles Compose estructurados.

## Catalogo, EPG y arranque

- El catalogo offline se descarga como snapshot firmado y filtrado por permisos.
- El catalogo total puede incluir miles de items live+VOD; esto no se considera corrupcion por si solo.
- La cache parseada de arranque se reutiliza cuando la huella remota no cambia.
- Si cambia la huella del snapshot, el primer arranque puede reconstruir cache y ser mas lento; el segundo arranque vuelve a ser rapido.
- La carga progresiva de EPG usa snapshot local dirigido para evitar fallback remoto canal a canal.
- Timeline EPG lee desde snapshot local dirigido incluso cuando el snapshot completo es grande.
- El backend EPG resuelve referencias por `channel_id`, `tvg_id` y nombre cuando el canal numerico del catalogo offline no coincide.

## Playback y trafico

- La prioridad sigue siendo trafico de video directo siempre que la plataforma lo permita.
- Movistar ISM usa rutas directas/proxy ligero segun perfil, manteniendo claves bajo demanda sin guardar keys en claro en catalogo.
- El dashboard diferencia trafico directo frente a trafico que realmente pasa por DRBEP cuando la app reporta la ruta correctamente.
- Orange esta integrado como plataforma offline y debe priorizar la mayor calidad soportada por el dispositivo.
- Pluto, Tivify, Runtime, Movistar, Movistar ISM y grupos especiales tienen rutas especificas ya ajustadas en varias tandas.

## Seguridad

- Las keys DRM sensibles ya no deben viajar en claro dentro del catalogo offline.
- La app resuelve licencias/keys bajo demanda con token de activacion y `device_id`.
- Deshabilitar un usuario o retirar permisos desde dashboard debe impedir nuevas reproducciones autorizadas aunque conserve una app instalada.
- Contenido adulto/protegido se controla con reglas desde dashboard y PIN local configurable en la app.

## Pendiente real

- Probar `2.0.320-beta-vod-hud-smoke-polish` desde el actualizador interno, no solo por ADB.
- Confirmar visualmente en Fire Stick real que `Guia` abre una sola vez y mantiene foco correcto.
- Valorar separar VOD pesado del catalogo de arranque para reducir el primer parseo tras cambio de snapshot.
- Revisar restos XML/layout legacy que sean decorativos o contenedores ya prescindibles.
- Afinar UX de grabaciones con filtros adicionales tipo dashboard: `Grabando`, `Fallidas`, `Completadas`.
- Reducir ruido de logs de imagenes/Glide y metricas externas cuando no aporten diagnostico.
- Revisar en tablet/movil que HUD tactil, selector de plataforma y barra timeshift no se solapen.
- Confirmar que Actividad offline del dashboard muestra calidad, programa actual, miniatura y consumo estimado en sesiones externas.

## Comandos utiles

```bash
./gradlew :app:compileReleaseJavaWithJavac
scripts/publish_offline_update.sh --channel beta --changelog "..."
adb -s 192.168.93.16:5555 install -r app/build/outputs/apk/release/app-release.apk
adb -s 192.168.93.16:5555 logcat -d -s DRBEP-TV-Native:W CatalogSnapshotStore:W EpgRepository:W PlayerController:W
```

## Documentos conservados

- `README.md`: informacion general del proyecto.
- `CHANGELOG.md`: historico de cambios versionados.
- `docs/offline-firestick.md`: guia operativa del modo offline/Fire Stick.
- `docs/offline-app-current-state.md`: este documento, fuente unica de estado actual y pendientes.
