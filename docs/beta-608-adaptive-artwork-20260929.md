# Beta 608 · Tarjetas adaptadas al arte

- Las tarjetas consultan arte promocional horizontal público cuando el proveedor
  e identificador están admitidos por HomePublicArtwork (Netflix y Apple TV).
- Si no hay imagen pública o falla su carga se usa la imagen del catálogo.
- El ancho es 244 dp para horizontales y 137 dp para verticales; la altura se
  calcula con la proporción real (límite defensivo 0,5–2,5).
- Título y subtítulo debajo de la imagen. Progreso en el borde inferior.
- Peticiones cancelables al liberar la vista; callbacks guardados por identidad.
- No se cambian sesiones, reproducción, backend ni publicación del canal general.

Release compilada y firmada. Cinco comprobaciones de foco/arte/carga local pasan.
Instalación -r en 192.168.93.190:5555 correcta; versionCode 608 confirmado.
Proceso actual: hero 1280×720, tarjetas 640×360, 480×270 y 320×480 cargadas,
sin AndroidRuntime fatal en la consulta. Verificación mediante versión instalada y logs. El intento de accesibilidad final
devolvió null root node; valoración visual en el televisor pendiente del usuario.

SHA release: 2d6caa554a73f292ac48ab6277f68af91103d56840516e261ec4a19f161a63a4
SHA APK .190: 7ab3af0c18fa62a9efd23cf8296de0701cd12dfb02a7ffeec30dc6ba8aefc0ac
Firma compatible y payload idéntico salvo META-INF verificados. APK 607 conservada.

Demo web privada, con datos ficticios de progreso y sin cuentas o reproducción:
https://drbep-portada-demo.bepllorens50167.chatgpt.site
Código independiente: /Users/bep/Documents/drbep/home-demo
La demo representa el criterio visual y navegación, no ejecuta el reproductor Android.
