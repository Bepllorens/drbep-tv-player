# Catchup M3U importado — 02/11/2026
Backend: /srv/drbep-dev/repos/drbep/docs/importacion-m3u-jwk-catchup-20261102.md.
Configuración catchup del catálogo: type, days, source. ChannelItem serializa estos campos; no cambia la base de datos local.
PlaylistCatchup incorpora start_iso/end_iso en UTC. ImportedChannelCatchup mantiene la licencia existente y evita interferir con Movistar/Orange. Acciones en guía, miniguía y portada; ventana y permisos comprobados antes de reproducir. Media3 resuelve archivo/seek, sin fallback a otro canal.
Compilación debug y 1.122 pruebas aprobadas. Publicación no solicitada. Candidata firmada de prueba785 en preparación con respaldo de la APK del Fire.
Pendiente playback real de plantilla, pausa/seek/retorno al directo; no dar por probado Vodafone por pruebas sintéticas.


## Validación física posterior — 2026-11-02

Se instaló en el Fire .190 una candidata privada ARM32 conservando la versión 785 y la firma de pruebas ya instalada, mediante actualización sin borrar datos. No se publicó una beta ni se modificó producción.

- APK: `/srv/drbep-dev/reviews/import-catchup-fire-20261102/candidate-fire.apk`.
- SHA-256: `8118b0c9bd1aedb87864dfd10ce7a942d8662a3ddb834c8d9cc0c4c2022de06c`.
- Respaldo para retorno: `/srv/drbep-dev/reviews/import-catchup-fire-20261102/before.apk`.
- La 1, Movistar ISM: directo con imagen; apertura del Telediario Matinal de hoy desde el carrusel; reproducción observada hasta 04:15 de 1:50:00; regreso al directo mediante su botón y después a Inicio con vídeo visible en la miniatura.
- Evidencias privadas en la misma carpeta: `la1-replay-request.png`, `replay-playing.png`, `return-live.png`, `return-home.png`.
- Esto valida una regresión acotada del catchup existente; no demuestra la reproducción de un canal Vodafone importado ni la ausencia de cortes prolongados. Vamos y la reproducción real de la nueva plantilla siguen pendientes.
- El soporte del backend permanece únicamente en desarrollo. Para probar la nueva plantilla de extremo a extremo falta activar ese soporte con autorización y sincronizar un canal autorizado con EPG y archivo disponible.
- El control del Mac seguía informando bloqueo al consultar su estado; las comprobaciones de Chrome del Mac no se dan por realizadas. La validación del editor se hizo con Chromium en el .185 y datos sintéticos.

## Resolución de identidad del archivo importado
Backend snapshot activado posteriormente con respaldo en import-catchup-snapshot-20261102. El Fire muestra Desde el inicio tras volver a seleccionar La 1 Vodafone. La apertura inicial quedó sin vídeo.
El archivo tiene id imported-catchup:<canal>:<inicio>; el controlador consultaba /api/stream con ese id sintético. Se añade streamInfoChannelId/fetchStreamInfoForRequest: consulta la información existente del canal de origen y conserva explícitamente la URL de archivo de la solicitud, sin reutilizar manifiestos ya parcheados del directo. Las solicitudes de otros perfiles mantienen su id. Prueba de identidad añadida, 1123 tests sin fallos. Pendiente comprobación de esta candidata en Fire al terminar la compilación.

Se detecta además que la caché binaria v17 omitía los campos importados. Se añade persistencia de type/source/days y versión18 para reconstruir las cachés16/17 desde el catálogo firmado guardado, sin borrar activación ni preferencias. La APK intermedia de identidad se instaló con firma de pruebas existente y actualización -r, SHA9cf66c16ebfd5abe59be0923ef3cbdc3313fb0286641846f5366afd9a262915c. No dar por validado el archivo hasta probar la candidata con persistencia corregida.

## Corrección y validación final — 2026-11-02
Causa efectiva del negro: PlaybackStreamInfoPolicy excluía cualquier channel.isVod, incluido el archivo importado. Por ello no se resolvía previamente la referencia existente del canal. Al habilitar esa resolución se debe consultar el id original (no el id sintético del programa), conservando la URL del archivo; se corrigieron ambas condiciones. Se fuerza también seekTo(0) para el inicio de imported_catchup, incluso cuando el MPD es dinámico, sin cambiar el comportamiento de los otros perfiles.

Archivos adicionales corregidos: PlaybackStreamInfoPolicy.java, CatalogSnapshotStore.java (caché v18 y campos importados), PlayerController.java. Test ImportedChannelCatchupTest ampliado. 1124 pruebas unitarias aprobadas; compilación release final exitosa.

APK optimizada instalada mediante actualización -r en Fire .190, versión785 privada de prueba, ARM32, misma identidad de firma de pruebas que la ya instalada. Ruta /srv/drbep-dev/reviews/import-catchup-snapshot-20261102/final-fire.apk. SHA256 707b3e1a289c3b84c2529118185c1daf772df2df86c308d6dfc959eefbabb042. Retorno a APK previa: /srv/drbep-dev/reviews/import-catchup-fire-20261102/candidate-fire.apk. Datos/activación conservados. No se publicó APK.

Comprobado físicamente en la release final:
- Desde el inicio visible al arrancar desde la caché, sin volver a sincronizar.
- Apertura del archivo de La 1 HD Vodafone con imagen y contador desde el comienzo.
- Pausa a01:13; avance de30s a01:43 conservando pausa; reanudación hasta01:47 con imagen.
- Botón TV del archivo devuelve al directo de La 1 HD; vuelta a Inicio conserva la miniatura con imagen.
- Evidencias: final-archive.png, final-seek-paused.png, final-pause-hud.png, final-paused-seek-hud.png, final-resumed.png, final-return-tv.png, final-return-home.png en el directorio de revisión.

La comprobación se limita a La 1 HD configurada: no extrapolar a los otros112 canales ni a archivos de fechas distintas. No se cambian las rutas de proveedores Movistar/Orange. Las pruebas físicas de Vamos no se han repetido en esta operación. Los cambios y documentos de este lote se consolidan en Git el 03/11 por petición expresa; beta/stable/rescue no se publicaron.
