# Tivify en Fire: diagnóstico del 03/11/2026

El usuario aparca el plan del dashboard y pide revisar Tivify, dejando La 1 sintonizada. Se captura la ejecución existente sin cambiar de canal ni instalar otra APK.

## Evidencia

- Fire .190 AFTKRT, Android API30; DRBEP versión785 / 2.0.785-beta-navigation-cache.
- Canal1103933, La 1, plataforma Tivify.
- 15:48:44–45: descarga de manifiesto e inicialización de pistas correctas; vídeo AVC 1280×720,50fps, decodificador OMX.MTK.VIDEO.DECODER.AVC.
- Ruta seleccionada: URL directa del origen, DASH, ClearKey, modo auto, fallback=false, allowCompat=false. No pasa por el manifiesto del proxy r58.
- Fallo al encolar vídeo protegido: MediaCodec CryptoException, Operation not supported in this configuration, error -2006; Media3 ERROR_CODE_DRM_SYSTEM_ERROR.
- La app reconstruye Media3 una vez y repite la misma ruta. Se reproduce exactamente el mismo fallo; no aparece primer fotograma.
- Consulta de metadatos del manifiesto actual: HTTP200, dynamic, protección cbcs y señalización Widevine en las cuatro adaptaciones examinadas. No se guardan claves ni cuerpos de manifiestos.
- La documentación oficial Media3 enumera ClearKey con cenc y Widevine con cenc/cbcs: https://developer.android.com/media/media3/exoplayer/drm

## Interpretación y límites

La combinación seleccionada por la app (ClearKey con contenido cbcs) es incompatible con la ruta nativa observada. El error concuerda con esa incompatibilidad, no con un agotamiento del búfer. No hay evidencia de una clave ausente y no se han inspeccionado/modificado claves.

El intento observado usa el origen directo, por lo que la modificación de cadencia del proxy r58 no interviene en ese manifiesto. No se dispone de una captura histórica equivalente que permita afirmar cuándo cambió el formato del origen o la selección de la app.

## Código relevante

- app/src/main/java/com/drbep/tvplayer/PlaybackRouteResolver.java: buildDecision y elección de ruta/DRM.
- app/src/main/java/com/drbep/tvplayer/PlayerController.java: tryAutoRecovery, recreación del reproductor tras error de decodificador, condiciones de respaldo.
- app/src/main/java/com/drbep/tvplayer/MainActivity.java: isPlaybackRepairEnabled.

## Continuación

Comprobar la configuración de origen y su ruta de reproducción compatible/autorizada para este dispositivo. Evitar que el modo automático trate cbcs como ClearKey nativo compatible o repita indefinidamente una configuración rechazada. La disponibilidad de Widevine requiere comprobar también el servicio de licencia correspondiente; la señalización en el MPD no basta para declarar reproducción válida.

Después de corregir la elección compatible, comprobar primer fotograma, audio y continuidad en La 1 Tivify, y regresión de las rutas actuales. No modificar claves ni cambiar globalmente el motor para resolver este caso.

## Estado

Diagnóstico completado; no se ha corregido ni se declara recuperada la reproducción. Sin cambios de código, instalación, publicación o despliegue. Plan del dashboard aparcado por indicación del usuario. Registro original privado y restringido en /srv/drbep-dev/reviews/tivify-fire-20261103/logcat-private.txt; no añadirlo a Git.

## Contraste con TeVas solicitado por el usuario

Se inspecciona la copia descargada de TeVas0.4.9 en /srv/drbep-dev/reviews/tevas-049-20261028/source. Su núcleo de reproducción contiene soporte propio para procesar CBCS en la app antes de que las muestras lleguen a MediaCodec. No depende del soporte CBCS del ClearKey nativo del dispositivo para ese recorrido.

Esto explica la diferencia arquitectónica con el intento directo de DRBEP observado. La incompatibilidad documentada es de la combinación nativa elegida, no una imposibilidad general del Fire para reproducir CBCS. La observación del usuario de que funciona en TeVas es coherente con el código examinado; no se ha realizado una prueba nueva de TeVas en el dispositivo en esta revisión.

Archivos de referencia en el árbol de TeVas: owntv-cineplex-core/player-core/src/main/java/tv/own/owntv/player/kodi/KodiMediaSourceFactory.kt y KodiManifestParsers.kt. El árbol declara GPLv3 en LICENSE; esta revisión no copia ni integra código de terceros.

La copia Git parcial de la revisión0.5.1 identifica HEAD408724a; no se presenta como inspección completa de esa versión porque la búsqueda requería descargar objetos adicionales. La evidencia concreta de la arquitectura anterior procede de0.4.9.

No se han modificado claves, configuración, reproducción, APK ni producción.

## Intento de corrección y respaldo

Consulta de solo lectura del catálogo activo: el canal1103933 de plataforma2 tiene custom_playback_profile=direct. Esto concuerda con la ruta observada; no se cambia ese campo. El streamInfo del intento contiene licencia vacía y ClearKey local, no una licencia Widevine configurada.

Se revisan las rutas existentes: la alternativa /live corresponde a StreamNM3U8DL; no equivale a una integración nativa Widevine. No se la ha ensayado ni se ha cambiado la selección del canal. Una solución nativa requiere una fuente y licencia Widevine autorizadas por el proveedor, o una fuente compatible ofrecida por este. El anuncio Widevine del manifiesto no proporciona por sí solo una licencia.

APK instalada respaldada en privado: /srv/drbep-dev/reviews/tivify-fire-20261103/fire-before.apk; SHA256707b3e1a289c3b84c2529118185c1daf772df2df86c308d6dfc959eefbabb042. No hubo instalación, cambio de claves, perfil, producción ni publicación. La reproducción continúa sin corregir.

## Comparación y ensayo posterior de selección (17:32–17:35)

El usuario confirma que ha detenido la otra IA. Se encuentran cambios ajenos en cinco archivos Android y una APK786 privada ya instalada. Se conservan íntegros en other-agent-before-live-trial.patch dentro de la carpeta privada de revisión.

Dashboard: dashboard.js selecciona /drm/manifest/{id} para DASH ClearKey; ProxyManifestDRM llama RewriteManifestKeepDRM y conserva la protección para el navegador. Android original seleccionaba el MPD de origen con ClearKey nativo. Por tanto, la ruta del dashboard no es una salida sin protección que pueda copiarse directamente al Fire.

El ensayo ajeno seleccionaba /decrypt/1103933: los registros conservados muestran un error de contenedor y posteriormente timeout. La prueba HTTP independiente que se dejó en la revisión obtuvo401; no demuestra por sí misma que la petición autenticada de la app tuviera ese mismo resultado.

Se ensaya únicamente La1 Tivify usando la ruta existente /live/1103933?client=firestick, sin modificar el backend, catálogo ni claves. Pasan30 pruebas de rutas y6 de recuperación. APK privada de ensayo SHA256d1167d58e015a392631c8296859f3b6ca0fac88736f0b0064b4ba0749644a71d. El Fire registra primer fotograma en10648ms y después1622 fotogramas renderizados sin descartes, PERO las capturas muestran imagen gris y corrupta, tanto en miniatura como a pantalla completa. No se considera playback válido ni corrección.

Se revierte el ensayo de código: cmp confirma que el diff vuelve exactamente al parche ajeno previo. Se reinstala con éxito, sin borrar datos, la APK786 previa SHA2566493b511294a433284f640525feca4f8305fdfc82d130624542a87f8b7c77626. No se publica ni se cambia producción. Los cambios ajenos pendientes no quedan validados por esta prueba.

El usuario aporta otro fallo: DAZN1 de plataformaDAZN. Se abre una investigación separada sobre posible regresión del proxy compartido. No inferir que todos los fallos tienen la causa CBCS de Tivify.

## Contraste DAZN y posible regresión compartida

DAZN1 (1114674) usa /proxy/manifest/1114674 con Widevine; el campo de catálogo es proxy_manifest. Arranque observado17:36:18.964, primer fotograma2462ms. El usuario confirma imagen y sonido en la TV y posteriormente que DAZN1, DAZN2 y otros antes fallidos ahora funcionan. La captura negra no prueba fallo: se trata de reproducción protegida y se contrasta con la observación del usuario. No se cambió el backend, la cuenta DAZN ni la configuración de canales para esta recuperación. Hubo reinstalación y arranque de la app durante el ensayo, pero no se atribuye causalidad sin reproducción controlada.

El cambio r58 sí afecta al reescritor DASH compartido, por lo que no es correcto descartar de forma general efectos en Android. Sin embargo, el intento original de Tivify iba al origen directo, y DAZN reproduce actualmente usando el proxy vigente. No se demuestra una regresión permanente de r58 con estas pruebas.

Los registros de DAZN1 contienen avisos403 de un proceso auxiliar al consultar la licencia del relay. Persisten durante la reproducción confirmada; no equivalen a un error fatal de la sesión Widevine nativa ni demuestran la causa del fallo previo. No se modifica ese proceso ni se inspeccionan claves. No hay un A/B del binario antiguo/nuevo que permita cerrar la causalidad.
