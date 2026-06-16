# Offline beta release notes

Notas acumuladas para promocionar la app offline de beta a stable.

## Pendiente de promocionar a stable

Rango actual: `2.0.82-beta-group-order` a `2.0.86-beta-zap-quality`.

### Navegacion y catalogo

- Los filtros de grupo respetan el orden especifico guardado en el dashboard para cada grupo.
- El snapshot offline incluye `group_order`, evitando mezclar el orden de plataforma con el orden de grupo.
- La app adopta el canal de actualizaciones efectivo devuelto por el servidor, por ejemplo cuando un dispositivo se mueve de stable a beta desde el dashboard.

### Actualizaciones

- El instalador de actualizaciones usa `ACTION_INSTALL_PACKAGE` como ruta principal en Fire OS.
- La beta publicada actualmente es `2.0.86-beta-zap-quality` (`versionCode 91`).

### Calidad real de reproduccion

- La app mide la calidad real del stream reproducido usando Media3: resolucion, codec de video, fps, bitrate y codec de audio.
- Los heartbeats de reproduccion envian la calidad real al servidor para verla en Actividad offline.
- El dialogo de diagnostico de reproduccion muestra la calidad detectada del canal actual.
- El overlay inferior completo muestra la calidad real junto al nombre, ruta y EPG.
- El aviso superior deja de repetir el nombre del canal cuando ya aparece en el panel inferior.
- El cartel inferior de zapping muestra la calidad real del canal y se refresca cuando Media3 detecta el formato.
- El cartel inferior de zapping permanece visible unos segundos mas para dar tiempo a ver la calidad detectada.

### Dashboard y observabilidad

- El dashboard muestra la calidad real en Actividad offline.
- El resumen por canal muestra la ultima calidad real observada.

## Comprobaciones antes de promocionar

- Verificar en un Fire Stick beta que el cartel inferior de zapping muestra `Calidad: detectando...` y despues valores reales como `1080p · H.264 · 50 fps`.
- Verificar que el aviso superior no repite el nombre del canal al entrar en READY.
- Verificar que Actividad offline del dashboard muestra `Calidad real`.
- Verificar que el resumen por canal del dashboard muestra `Ultima calidad`.
- Confirmar que la actualizacion beta instala correctamente y reporta version `91`.
