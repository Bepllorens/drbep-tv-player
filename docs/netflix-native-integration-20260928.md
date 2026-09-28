# Netflix nativo en la app offline — beta 599

El catálogo privado abre NetflixPlaybackActivity desde películas/episodios,
respetando allowsNetflixVod y validando un identificador numérico positivo.
La actividad no está exportada. Mantiene una identidad propia de instalación,
solicita vinculación por código cuando falta sesión y guarda las cookies con
Android Keystore/AES-GCM en almacenamiento privado (backup deshabilitado).
No importa la sesión de la aplicación prototipo.

El transporte MSL y las licencias utilizan el CDM nativo del dispositivo.
La implementación se limita actualmente al Fire TV AFTKRT/systemId 27925
validado; no inventa identidad de otro modelo ni utiliza WVD. Solicita perfiles
HEVC Main10 tras comprobar el decodificador seguro. Usa los perfiles H264 del
manifiesto base cuando no se solicita UHD. No hay fallback a extracción de claves.

Media3 obtiene la licencia usando la ruta del propio manifiesto y reproduce
las pistas de vídeo y audio mediante MergingMediaSource. Se elige la mayor
resolución ofrecida y la variante de menor bitrate de esa resolución; no hay
adaptación dinámica de bitrate entre variantes. Se prioriza audio es-ES/es.
PlayerView aporta pausa, desplazamiento y controles de mando. Guarda posición
por título al salir y la borra al terminar. Al pasar a segundo plano libera
la reproducción; las peticiones de vinculación se cancelan al cerrar la pantalla.
Errores muestran reintento y opción de volver a vincular la cuenta.

Validación previa: prototipo en el mismo Fire Stick con vídeo 3840x2160 y audio
es-ES, 1058 fotogramas sin omitidos y 1039 buffers de audio durante 44 segundos.
Un bloqueo anterior también afectaba a PCM local sin DRM y se resolvió reiniciando
el dispositivo. No se añade ningún reinicio automático ni workaround que simule
el reloj de audio.

Compilación debug y 534 pruebas unitarias correctas (incluye límites de URL del
nuevo lector de manifiesto). La validación de reproducción de la app integrada
requiere vincular la cuenta en esta instalación. El éxito del prototipo no prueba
por sí solo la sesión de la app principal. No se ha publicado un canal de actualización.


La compilación release 599 también supera lintVital y R8. Certificado de
publicación verificado: 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b.
APK de distribución SHA-256: 21e41ea97e489f8c95c145a49b6a032362fdb1f068d56b5fc465f79374cf2da3.
El Fire Stick tenía beta 598 firmada con el certificado de desarrollo
9f76a326703c87666c247a743679b5bf696aacf5f34d835bea53d369fcdfd200.
La instalación inicial con firma de distribución fue rechazada sin cambiar
la app. Se preparó para este dispositivo el mismo APK release con la firma
compatible, verificando identidad de todo el payload salvo META-INF/firma.
SHA-256 del APK compatible: 07ab43fd1d8ecf4d22e2c9df4294ec29a3858d2279763c959da417ffe3051f76.
Respaldo del APK 598 guardado en /tmp/drbep-netflix-integration/offline-598-backup.apk.


## Resultado final de instalación y acceso por catálogo

La validación de UI detectó que PrivateVodBrowser seguía dependiendo de la
capacidad de reproducción remota del descriptor. Se añadió una capacidad
nativa explícita para Netflix, separada del descriptor remoto y subordinada
a allowPlayback y al control allowsNetflixVod en MainActivity. Los demás
proveedores conservan su lógica anterior. Se repitieron las pruebas del
navegador privado y de NetflixMediaUrls, y la compilación release/lint/R8.

Artefactos finales, que sustituyen los hashes preliminares anteriores:
- Distribución: ceff4a6b4e6427fbcdc1059fd1db6f77bb0c41bb386fb0715bf516292ba8d107.
- Compatible con el Fire Stick de pruebas: 2e265ba07e106a7f72919f1847188594efbf5f105ede8b5dabde427219f12458.
Se comprobó identidad del payload salvo firma y hash en el Fire Stick antes
de `pm install -r`, que terminó con Success. VersionCode 599 confirmado.
La instalación incremental fue rechazada por Fire OS; se usó copia completa.

Recorrido de UI verificado: inicio → VOD → Origen → Netflix → Unabomber.
El catálogo ya no indica Solo consulta y la ficha abre la pantalla de
vinculación de NetflixPlaybackActivity con un código propio de la app.
El código no se registra en este documento. El usuario confirmó la vinculación
y se verificó después la reproducción en esta instalación, como se detalla abajo.
No se publicó la beta en el canal general, ni se hizo commit/push.


## Verificación real después de vincular la app integrada

El usuario confirmó que había introducido el código. La actividad pasó al
reproductor de Unabomber dentro del paquete com.drbep.tvplayer.offline (599).
Las comprobaciones del sistema, sin capturar vídeo protegido, mostraron:

- Decodificador OMX.MTK.VIDEO.DECODER.HEVC.secure en el proceso de la app.
- SurfaceView de NetflixPlaybackActivity con activeBuffer 3840x2160 y
  hasProtectedContent=true; es el búfer de vídeo, no solo la resolución de pantalla.
- SurfaceFlinger: 127 fotogramas presentados en una ventana de 5,25 segundos
  (aproximadamente 24 fps), confirmando avance real del vídeo.
- AudioFlinger: pista activa de la app, PCM estéreo a 48000 Hz, contador
  avanzando de 0x00CD7D00 a 0x00D99800 y 0x00E9C100, sin underruns en esas lecturas.

La reproducción integrada UHD y la salida activa de audio quedan verificadas
por telemetría del dispositivo. No se ha comprobado subjetivamente el sonido en
la habitación ni se ha probado en esta pasada la reapertura y reanudación tras
cerrar la actividad. Se deja el título reproduciendo. No hay publicación general
ni commit/push como parte de esta validación.
