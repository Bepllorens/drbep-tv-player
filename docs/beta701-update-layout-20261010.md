# Beta 701 — aviso de actualización

La foto IMG_5058 muestra el aviso disponible en beta699/700, con versiones largas y un único párrafo de novedades. Se separan las versiones en filas, las novedades en viñetas y la información de instalación en otro bloque. Se amplía el ancho máximo a 560 dp y el relleno a 28 dp; interlineado explícito, separación vertical, botones apartados y fondo oscuro con degradado. El contenido tiene altura acotada y puede desplazarse en pantallas pequeñas. Se mantienen los avisos breves y la política de actualizaciones obligatorias.

Las versiones del aviso se muestran sin el sufijo técnico; las novedades se normalizan y limitan a tres entradas de hasta 180 caracteres. Las nuevas notas de publicación usan tres frases breves.

Build debug y release/R8 correctos. Firma oficial SHA-256 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b. APK universal 701, 2.0.701-beta-update-layout, SHA-256 8521c251cedb6e6d2a0a6f86d55abb4710b58f7075eae8894717de1ad68fcc3d. Publicación beta y descarga pública verificadas. Respaldo /opt/drbep-releases/beta701-publication-20261010; stable582/rescue543 iguales. Sin instalación por ADB ni validación visual en dispositivo durante este trabajo.

## Investigación U7D separada

La guía local del canal ISM1105451 usa el alias Movistar Plus HD; la identidad original es MPLUS, igual que las fuentes DASH y HLS. Los títulos y el inicio del programa Bakalá coinciden con la guía oficial. Hay diferencias de hora de fin entre guías, pero una petición con el intervalo oficial también falló en la ruta antigua.

Comparación con el mismo token lineal válido: La1/4521 devuelve MPD200 en cutv-wp10.cdn.telefonica.com; MPLUS/5240 devuelve404. El resolutor oficial especificó «device type mismatch»: sesión webplayer y ruta webplayer.hls. Con una renovación normal aislada para webplayer.hls y su mv_token, el resolutor devuelve200 y un MPD en cutvhcsmno-wp0.cdn.sv.video.telefonicaservices.com/5240/vxfmt=dp/Manifest.mpd. Este MPD también devuelve200 con el token del directo; no necesita sustituir el token lineal para descargarlo.

Los identificadores de pista de ese archivo no coinciden con las claves ya configuradas para el canal ni con el caché de archivo existente. No se han solicitado licencias ni extraído claves en estas pruebas. No se ha cambiado la sesión ni desplegado código del backend. La reproducción U7D de Movistar Plus sigue pendiente; acceder al MPD no prueba reproducción. Hace falta integrar y validar la reproducción con licencia nativa autorizada para esa ruta; una modificación de EPG por sí sola no acredita la reparación.
