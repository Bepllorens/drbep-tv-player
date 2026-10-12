# Beta 700 — panel web del dispositivo

Se ha inspeccionado el panel de la APK TeVas 0.2.33 aportada por el usuario: assets/webpanel/index.html, app.css y app.js. Se adapta su organización (menú lateral, resumen, listas, importación, actividad y diagnóstico), con implementación propia y paleta DRBEP; no se reutiliza código, marcas ni fuentes de OwnTV.

El panel funciona con un único HTML local sin dependencias externas. Resumen con estado del reproductor, resolución, memoria, almacenamiento y número de listas. Gestión por filas con búsqueda, filtros TV/película/serie/otros, favoritos, ocultos, paginación y edición mediante diálogos accesibles. La selección de lista se conserva al refrescar; las peticiones antiguas no sustituyen resultados nuevos y la consulta periódica no reconstruye títulos sin cambios.

Importación por archivo con arrastre, progreso y límite de 256 MB. Nueva acción autenticada import-url valida nombre y URL HTTP(S), crea fuente, configura XMLTV/actualización automática y encola el mismo WorkManager que la app. Si falla el encolado elimina únicamente la fuente sin publicar. source-info devuelve solo la opción automática, sin exponer localizadores o credenciales. La edición de títulos conserva su orden actual.

Se mantienen PIN, protección de origen/Host, caducidad de veinte minutos y autenticación de todas las operaciones. No modifica el backend, la reproducción ni las bases de datos estables. Incluye las correcciones ya publicadas en beta699.

Validación: DeviceWebPanelTest (3 pruebas, sin errores), build debug; integración Playwright con servidor local de fixtures (PIN incorrecto/correcto, texto de títulos sin ejecutar HTML, navegación, filtros, paginación, diálogos, retención de selección, importación URL/archivo, diseño móvil sin desbordamiento y sesión expirada). Capturas de escritorio/móvil con datos de ejemplo en artifacts/beta700 del backend. Compilación release, lint y firma: ver informe de publicación. La prueba de navegador usa fixtures; no demuestra por sí sola una importación en un dispositivo real.

Para probar: actualizar a beta700; Ajustes → Listas personales → Panel web del dispositivo; abrir el QR y conectar con el PIN. Debe cerrarse y abrirse de nuevo el panel para servir el HTML nuevo después de actualizar la app.

## Publicación verificada

Release universal 700, 2.0.700-beta-device-panel; SHA-256 ac0d2731a2284bdfc80b718b2eacc96dca31dc7d412651354bd41c8e237a572b; certificado oficial SHA-256 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b. Preflight y publicación correctos; metadatos en fire.tvbep.com e iptv.bepllorens.com y hash de descarga pública comprobados. Respaldo /opt/drbep-releases/beta700-publication-20261010. Stable 582 y rescue 543 se mantienen idénticos.

assembleRelease y R8 correctos. Lint global falla: 184 errores y 226 avisos; primer error MissingSuperCall en NetflixPlaybackActivity sin cambios en este trabajo. El informe no muestra errores en el nuevo controlador/panel; un aviso previo UsableSpace afecta la consulta de almacenamiento de MainActivity. No se ha suprimido lint ni se declara limpio. No se ha instalado por ADB ni interrumpido la reproducción del usuario: la beta se obtiene con el actualizador.
