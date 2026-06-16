## 2.0.86-beta-zap-quality
- Offline beta: el cartel inferior de zapping muestra la calidad real del canal y se refresca cuando Media3 detecta el formato.
- Offline beta: el cartel de zapping permanece visible unos segundos mas para dar tiempo a ver la calidad detectada.

## 2.0.85-beta-overlay-quality
- Offline beta: el overlay inferior muestra la calidad real del canal junto al nombre, ruta y EPG.
- Offline beta: el aviso superior deja de repetir el nombre del canal cuando ya aparece en el panel inferior.

## 2.0.84-beta-playback-quality
- Offline beta: la app mide la calidad real del stream reproducido (resolucion, codec, fps, bitrate y audio) usando Media3.
- Offline beta: los heartbeats de reproduccion envian la calidad real al servidor para verla en Actividad offline.
- Offline beta: el dialogo de diagnostico de reproduccion muestra la calidad detectada del canal actual.

## 2.0.83-beta-update-channel
- Offline beta: la app adopta el canal efectivo devuelto por el servidor cuando el dashboard asigna beta a un dispositivo.
- Offline beta: el instalador de actualizaciones usa ACTION_INSTALL_PACKAGE como ruta principal en Fire OS.

## 2.0.82-beta-group-order
- Offline beta: los filtros de grupo usan el orden especifico guardado en el dashboard para ese grupo.
- Servidor: el snapshot offline incluye orden por grupo (`group_order`) para evitar mezclarlo con ordenes de plataforma.

## 2.0.73-offline-filter-permissions
- Offline: oculta el filtro virtual "Todos" para que la navegacion use solo plataformas, grupos y VOD autorizados.
- Offline: el catalogo respeta grupos permitidos como filtro adicional cuando el usuario tiene grupos asignados.

## 2.0.72-offline-catalog-hotfix
- Offline: reduce el pico de memoria al guardar catalogos descargados, evitando cierres durante sincronizaciones remotas grandes.
- Offline: mejora la aplicacion de catalogos recortados por permisos de usuario conservando el ultimo catalogo bueno si falla la descarga.

## 2.0.71-offline-wipe
- Offline: la app obedece ordenes remotas de borrado local, detiene reproduccion y elimina catalogo, URL y token del dispositivo.
- Servidor: deshabilitar o borrar un usuario revoca sesiones y deja pedido de borrado local para sus dispositivos offline.
- Dashboard: nuevos controles de borrado local remoto por usuario, dispositivo y lote filtrado.

## 2.0.70-offline-observability
- Offline: cuando una ruta alternativa queda aprendida, la app envia un heartbeat `recovered` para alimentar el resumen por canal.
- Servidor: observabilidad offline muestra canales con fallos, recuperaciones, rutas usadas y tiempos de imagen.
- Telegram: nuevo comando `/playback [plataforma]` para consultar canales offline problematicos o recuperados.

## 2.0.69-offline-smart-route
- Offline: la auto-reparacion aprende rutas alternativas cuando el reproductor llega a READY tras un fallo, tambien en modo independiente.
- Offline: las rutas recuperadas se reportan al servidor con canal, plataforma, ruta elegida y motivo de recuperacion.
- Playback: se evita reaprender rutas en auto hasta que una alternativa real haya arrancado correctamente.

## 2.0.68-offline-catalog-guard
- Offline: el catalogo descargado se rechaza si pierde demasiados canales sin cambio de permisos, conservando el ultimo catalogo bueno.
- Offline: Sistema offline y reportes remotos muestran el ultimo bloqueo de catalogo con conteos y motivo.

## 2.0.57-offline-update-channel
- Offline: anadido selector de canal de actualizaciones stable/beta/rescue desde Herramientas y Sistema offline.
- Offline: las comprobaciones de actualizacion, diagnosticos y reportes al dashboard usan el canal elegido en el dispositivo.
- Offline: el resumen de Sistema offline muestra el canal activo para saber si el Fire Stick esta en estable, beta o rescate.

## 2.0.56-offline-clean-install
- Offline: tras una instalacion limpia aparece un flujo claro para activar con codigo como accion principal.
- Offline: al aprobar la activacion se descarga el catalogo automaticamente y se muestra resumen de usuario, permisos, canales, VOD y EPG.

## 2.0.55-offline-playback-repair
- Offline: si un canal falla por ruta/token/catalogo, la app fuerza una actualizacion de catalogo con fallback al ultimo catalogo bueno.
- Offline: la auto-reparacion de playback tambien prueba ruta directa en modo independiente cuando falla una ruta DRM/proxy.

## 2.0.32-offline-playback-presence
- Offline: la app reporta al servidor que canal o VOD esta reproduciendo cada usuario con heartbeat periodico.
- Servidor: anadidos endpoints admin para ver reproducciones activas e historial reciente.
- Telegram: anadidos `/estado`, `/viendo` y `/historial usuario`.

## 2.0.31-offline-movistar-logos
- Offline: normalizados los logos de Movistar a HTTPS antes de cargarlos para evitar fallos de Glide con las URLs HTTP antiguas guardadas en snapshots locales.
- Servidor: actualizadas las URLs base de logos Movistar a HTTPS para que los nuevos catalogos ya salgan corregidos.

## 2.0.30-offline-fresh-catalog
- Offline: el catalogo se considera obsoleto a los 30 minutos para evitar IDs caducados tras refrescos de plataformas como Movistar.
- Offline: al volver a la app se refresca el catalogo en segundo plano si esta viejo, sin esperar al temporizador de seis horas anterior.

## 2.0.29-offline-update-fallback
- Offline: el chequeo de actualizacion reintenta por LAN contra `192.168.93.223:8080` si el dominio publico se queda colgado.
- Offline: aumentado el timeout del endpoint de version para evitar falsos negativos en Fire OS.
- Offline: al volver a la app tambien se comprueba actualizacion de forma espaciada, no solo al crear la actividad desde cero.

## 2.0.28-offline-recovery
- Offline: anadido preflight antes de instalar actualizaciones para detectar paquete incorrecto, version no superior o firma incompatible antes de abrir el instalador.
- Offline: guardado diagnostico persistente del ultimo intento de actualizacion con etapa, version objetivo, APK e instalacion actual.
- Offline: recuperacion guiada cuando falta catalogo, URL o token, con acciones directas para activar por codigo, refrescar catalogo, ver estado o configurar credenciales.
- Offline: el reporte de estado al dashboard incluye mas senales de mantenimiento, catalogo y diagnostico de actualizacion.

## 1.4.24
- Fire Stick: herramientas y pantalla principal usan la misma version de `BuildConfig`, evitando titulos desfasados.
- Fire Stick: el script de instalacion desactiva paquetes antiguos conocidos para no abrir builds duplicadas desde el launcher.
- Dev: anadido script de auditoria de instalacion para comprobar paquete activo, version y duplicados DRBEP.

## 1.4.19
- Fire Stick: se retira Bouquet favoritos del menu Herramientas ahora que Favoritos ya existe como bouquet real en la navegacion.

## 1.4.18
- Fire Stick: el bouquet Favoritos queda siempre visible aunque haya filtros de arranque activos, para que no desaparezca del player.

## 1.4.17
- Fire Stick: Favoritos pasa a tener bouquet propio y Herramientas entra en ese grupo en vez de usar un modo separado de zapping.

## 1.4.16
- Fire Stick: Herramientas anade acceso rapido a Canales favoritos y un conmutador claro para zapping solo entre favoritos.

## 1.4.15
- Fire Stick: multiview sustituye el badge AUDIO por un marco naranja en la ventana activa, mas claro en tele.

## 1.4.14
- Fire Stick: multiview muestra ahora un badge AUDIO separado y un foco mas claro en la ventana activa, sin mezclarlo con el nombre del canal.

## 1.4.13
- Fire Stick: con la barra de timeshift visible, DPAD arriba vuelve a LIVE y DPAD abajo la cierra sin abrir otros paneles.

## 1.4.12
- Fire Stick: OK sobre reproduccion normal ahora muestra la barra de timeshift en vez de ocultarla tras pausar, y BACK la cierra primero si estaba visible.

## 1.4.11
- Fire Stick: cuando la barra de timeshift esta visible, LEFT/RIGHT del mando hacen seek sobre esa barra en vez de abrir el overlay.

## 1.4.10
- Fire Stick: mantener pulsado FF en timeshift ahora detecta la repeticion del mando y salta directo a LIVE, en vez de avanzar tramo a tramo.

## 1.4.9
- Fire Stick: pulsacion larga en FF dentro de timeshift vuelve rapido a LIVE y mantiene la barra visible para confirmar el salto.

## 1.4.8
- Fire Stick: primera base de timeshift con mando. PLAY/PAUSE y REW/FF muestran la barra y permiten pausar o mover el directo cuando el canal soporta DVR.

## 1.4.7
- Fire Stick: corregido el salto de Siguiente en la timeline para que abra la franja correcta del siguiente programa del canal y no una fecha invalida.

## 1.4.6
- Fire Stick: anadido boton Siguiente en la timeline para saltar al siguiente programa del canal enfocado sin recorrer toda la fila.

## 1.4.5
- Fire Stick: los chips Ahora, +2h y Cerrar de la timeline ya son accesibles con mando y se pueden enfocar desde la primera fila con DPAD arriba.

## 1.4.4
- Fire Stick: el boton Ahora de la timeline salta de verdad a la franja actual en vez de reabrir la ventana anterior.
- Fire Stick: la timeline recuerda mejor el bloque enfocado y al reabrirse intenta caer en el programa en emision o en el ultimo punto usado.

## 1.4.3
- Fire Stick: el menu de acciones de un programa en timeline o EPG visual ahora ofrece cancelar la grabacion si ese evento ya estaba programado.
- Fire Stick: cancelar una grabacion programada desde timeline refresca la propia vista al momento.

## 1.4.2
- Fire Stick: anadidos presets 1/2/3 para multiview desde Herramientas.
- Fire Stick: puedes guardar el multiview actual o reabrir un preset guardado sin reconstruirlo a mano.

## 1.4.1
- Actualizado RecyclerView a 1.4.0 sobre la nueva base de build con API 36.

## 1.4.0
- minSdk sube a 23 para alinearse con las dependencias modernas de reproduccion.
- Actualizadas las dependencias de reproduccion a Media3 1.10.0.
- Leanback pasa a la release estable 1.2.0.

## 1.3.9
- Migrado el build a AGP 8.9.1 y Gradle 8.11.1 para soportar compileSdk 36.
- compileSdk sube a 36 sin cambiar todavia el targetSdk, preparando el proyecto para actualizar Media3 y otras dependencias.

## 1.3.8
- Fire Stick: al pulsar BACK durante una grabacion ahora pregunta si quieres salir de la grabacion y volver al canal anterior.
- Fire Stick: al confirmar la salida de una grabacion vuelve al canal lineal que estabas viendo antes en lugar de quedarse bloqueado.

## 1.3.7
- Fire Stick: al reabrir una grabacion pregunta si quieres continuar desde el punto guardado o empezar de nuevo.
- Fire Stick: pulsar BACK durante una grabacion vuelve al panel de grabaciones y guarda el punto de reproduccion.

## 1.3.6
- Fire Stick: timeline recuerda ventana y canal enfocado al reabrir.
- Fire Stick: EPG visual recuerda la tarjeta enfocada al volver a abrir.
- Fire Stick: grabaciones recuerda el modo y el item seleccionado.

## 1.3.5
- Fire Stick: zapping circular dentro del grupo visible.
- Fire Stick: recuperacion automatica de reproduccion en modo auto probando rutas alternativas antes de fallar.

# Changelog

## 1.3.4 - 2026-03-31

- Added Fire Stick visual EPG sections backed by the same EPG category endpoints used by the dashboard.
- Kept the original timeline guide stable while separating the new visual EPG experience.
- Improved Fire Stick visual EPG card sizing, poster centering, section ordering, and DPAD navigation.
- Filtered visual EPG content by the current platform context and excluded sports from the live-now rail.
- Refined Fire Stick visual EPG focus movement between rows and horizontal channel rails.

## 1.3.3 - 2026-03-26

- Added Fire Stick multiview 2x2 entry from Tools.
- Added remote navigation inside multiview: DPAD move, OK audio/fullscreen, MENU change channel, BACK close.
- Improved multiview stability by using TextureView for the four preview players.
- Completed multiview slot selection with global live channels when the visible filter has fewer than four.
- Fixed the multiview channel picker for Fire Stick focus and scrolling.
- Added clearer focused-row highlight in the multiview channel picker.
- Added logo prefetching to make multiview channel picker artwork appear faster.
- Refreshes the timeline immediately after scheduling so programmed events are marked in place.
- Hides touch-only quick access chips and favorite star in Fire Stick overlay.
