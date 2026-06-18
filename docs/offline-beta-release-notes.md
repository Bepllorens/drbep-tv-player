# Offline beta release notes

Notas acumuladas para promocionar la app offline de beta a stable.

## Pendiente de promocionar a stable

Rango actual: `2.0.82-beta-group-order` a `2.0.91-beta-device-traffic-thumbnails`.

### Navegacion y catalogo

- Los filtros de grupo respetan el orden especifico guardado en el dashboard para cada grupo.
- El snapshot offline incluye `group_order`, evitando mezclar el orden de plataforma con el orden de grupo.
- La app adopta el canal de actualizaciones efectivo devuelto por el servidor, por ejemplo cuando un dispositivo se mueve de stable a beta desde el dashboard.

### Actualizaciones

- El instalador de actualizaciones usa `ACTION_INSTALL_PACKAGE` como ruta principal en Fire OS.
- La beta publicada actualmente es `2.0.91-beta-device-traffic-thumbnails` (`versionCode 96`).

### Calidad real de reproduccion

- La app mide la calidad real del stream reproducido usando Media3: resolucion, codec de video, fps, bitrate y codec de audio.
- Los heartbeats de reproduccion envian la calidad real al servidor para verla en Actividad offline.
- El dialogo de diagnostico de reproduccion muestra la calidad detectada del canal actual.
- El overlay inferior completo muestra la calidad real junto al nombre, ruta y EPG.
- El aviso superior deja de repetir el nombre del canal cuando ya aparece en el panel inferior.
- El cartel inferior de zapping muestra la calidad real del canal y se refresca cuando Media3 detecta el formato.
- El cartel inferior de zapping permanece visible unos segundos mas para dar tiempo a ver la calidad detectada.

### Estabilidad EPG

- Timeline EPG abre una ventana limitada de canales vivos alrededor del canal actual para evitar cierres por ANR en Fire TV.
- Visual EPG limita las tarjetas iniciales por seccion y prioriza el canal seleccionado para abrir mas rapido en plataformas grandes.
- Las acciones de grabacion desde EPG visual/timeline usan el token offline y ya no se bloquean solo por estar en modo independiente.
- Al programar desde una guia se muestra estado de progreso y se marca el programa como programado en la guia abierta.
- Timeline permite programar/cancelar directamente con la tecla menu sobre un programa y deja trazas claras de diagnostico.

### Dashboard y observabilidad

- El dashboard muestra la calidad real en Actividad offline.
- El resumen por canal muestra la ultima calidad real observada.
- Actividad offline muestra miniatura del programa o logo del canal en "Viendo ahora".
- Actividad offline muestra el programa actual cuando hay EPG disponible.
- Actividad offline estima el consumo por sesion y separa el trafico que pasa por el servidor DRBEP.

### Grabaciones

- La reproduccion de grabaciones desde la app offline anade el token de activacion a la URL de remux.
- Si un usuario no tiene permiso de programar grabaciones, la app muestra un aviso claro en lugar de parecer que no hace nada.

## Comprobaciones antes de promocionar

- Verificar en un Fire Stick beta que el cartel inferior de zapping muestra `Calidad: detectando...` y despues valores reales como `1080p · H.264 · 50 fps`.
- Verificar que el aviso superior no repite el nombre del canal al entrar en READY.
- Verificar que Actividad offline del dashboard muestra `Calidad real`.
- Verificar que el resumen por canal del dashboard muestra `Ultima calidad`.
- Abrir `EPG timeline` y `EPG visual` en un Fire Stick beta desde plataformas grandes y confirmar que no vuelve al launcher.
- Programar una grabacion desde `EPG visual` y confirmar que aparece en grabaciones programadas del servidor.
- Programar una grabacion desde `EPG timeline` usando la tecla menu sobre un programa y confirmar que aparece en grabaciones programadas del servidor.
- Reproducir una grabacion completada desde un Fire Stick externo y confirmar que arranca.
- Confirmar que Actividad offline muestra miniatura, programa actual y consumo estimado en "Viendo ahora".
- Confirmar que la actualizacion beta instala correctamente y reporta version `96`.
