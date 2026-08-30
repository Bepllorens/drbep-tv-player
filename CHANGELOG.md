## Unreleased - adaptive performance and cross-device UX
- Multiview: el selector de cada ventana permite elegir cualquier canal TV del catálogo autorizado, aunque pertenezca a otra plataforma, sin duplicar canales ya abiertos.
- Releases: permite publicar el APK ya probado sin recompilar y bloquea la entrega si su version, SHA o certificado no coinciden.
- Autorreparacion: separa de la Activity la decision de volver a automatico o probar la siguiente ruta, evitando reintentos en bucle para auto, proxy, compatibilidad y reproducciones directas.
- Rutas de reproduccion: centraliza cuándo descartar una ruta aprendida tras un fallo o una recuperacion por otro modo, respetando configuraciones manuales y overrides temporales.
- Recuperacion offline: desacopla de la Activity la decision de refrescar el catalogo tras errores de autorizacion, URL caducada o canal ausente, conservando el limite de un intento cada diez minutos.
- Grabaciones: consolida reintentos y artefactos físicos bajo una sola tarjeta, abre una ficha de acciones con OK y elimina de forma fiable todos los registros completados relacionados.
- Portada: muestra Biblioteca VOD desde el recuento del snapshot aunque el catálogo rápido inicial todavía contenga solo TV.
- Prime Live: recupera una sola vez los manifiestos DASH obsoletos mientras el relay mantiene estable la sesión del canal.
- HUD moderno: centra la fila de botones dentro del panel cuando caben y conserva desplazamiento horizontal cuando el número de acciones supera el ancho disponible.
- HUD: migra una sola vez las instalaciones existentes al diseño moderno; cualquier cambio posterior a clásico vuelve a conservarse.
- DAZN VOD: el selector indica que el catálogo se cargará al elegirlo, en vez de mostrar engañosamente cero títulos antes de la carga diferida.
- Biblioteca VOD: el origen abre un selector directo con la selección actual y el recuento visible, manteniendo la carga diferida de Movistar y DAZN.
- Apariencia: Herramientas permite alternar y recordar las paletas Aurora violeta, Grafito cian y Esmeralda carbón en portada, HUD, guía, listas y menús.
- VOD remoto: Plex y Movistar usan el backend publico del dispositivo offline, y los manifests Movistar conservan el host exterior en todos sus segmentos.
- Biblioteca VOD: abre inmediatamente el catalogo guardado y actualiza Movistar solo al seleccionar ese origen o buscar expresamente.
- HUD VOD: muestra la pelicula o episodio reproducido, su metadata y progreso en lugar del filtro o plataforma de television.
- Inicio U7D/VOD: conserva el panel de preparacion hasta recibir el primer fotograma, muestra la fase de buffer inicial y amplía solo para VOD el margen de una primera renovación DRM lenta.
- U7D Movistar: libera de forma determinista la fuente Media3 al volver a directo; el backend limita el replay a tiempo real y corta clientes bloqueados para que una sesión abandonada no afecte a otros usuarios.
- Arranque: alinea la plataforma visible con el ultimo canal restaurado para no volver aparentemente a Tivify cuando se estaba viendo otra plataforma.
- VOD externo: los catalogos offline usan el host publico del snapshot para VOD, Plex y U7D, evitando rutas privadas fuera de la red local.
- HUD: añade un acceso VOD inmediatamente después de U7D en los canales en directo, sin duplicarlo durante la reproducción VOD.
- HUD: U7D y VOD retiran correctamente los controles antes de abrir sus paneles para evitar que el HUD tape o cierre el destino.
- Grabaciones: abre el primer listado disponible sin esperar innecesariamente al alternativo y envía también la identidad del dispositivo al backend.
- Actualizacion offline: vuelve a ofrecer cada 24 horas una version opcional pendiente y recupera instalaciones antiguas cuyo aviso habia quedado silenciado sin fecha.
- Móvil: recupera el acceso a Herramientas dentro de la línea de progreso del banner automático, sin restaurar la botonera completa ni aumentar su altura con otra fila.
- Móvil: el banner automático de zapeo vuelve a ser informativo y compacto, sin botones; limita los textos a una línea para evitar recortes con títulos largos. Tablet y TV conservan las acciones.
- Zapeo: restaura el banner automático a una sola fila de acciones en móviles panorámicos para recuperar su altura original.
- Arranque: vuelve al último canal lineal reproducido, actualiza la caché rápida tras el primer fotograma y evita que VOD, U7D o otra sesión sustituyan la preferencia local válida.
- Móvil: el banner automático al cambiar de canal conserva tipografía, logo y controles de tamaño cómodo, distribuyendo las acciones en varias filas cuando hace falta.
- Móvil: el HUD inferior táctil aprovecha todo el ancho útil en teléfonos panorámicos, sin alterar el tamaño acotado de tabletas y TV.
- Empaquetado universal: incluye `armeabi-v7a` para Fire TV y `arm64-v8a` para móviles, tabletas y Android TV modernos.
- Guía TV: virtualiza las filas y conserva el foco cerca del canal activo para evitar componer toda la parrilla en el hilo principal.
- Navegación: presenta Directo, Guía, Grabaciones y Biblioteca como destinos principales antes de las herramientas secundarias.
- Diseño TV: refuerza foco, contraste y legibilidad de la guía y de la portada mediante los tokens visuales compartidos.
- Preferencias: sincroniza con la web app favoritos, recientes, último canal, progreso VOD y presets multipantalla por usuario.
- Audio y subtítulos: recuerda la lengua seleccionada y si los subtítulos deben permanecer activados.
- Seguridad: cifra el token de acceso con Android Keystore y migra automáticamente instalaciones anteriores.
- Control parental: sustituye el hash rápido del PIN por PBKDF2 y bloquea temporalmente los intentos repetidos.
- Compatibilidad: incorpora PiP y búsqueda por voz con detección de capacidad y fallback seguro.
- Soporte: permite enviar desde el dispositivo un paquete de diagnóstico saneado al dashboard.
- Calidad: corrige el manifiesto debug, activa reglas de extracción seguras y añade un smoke test instrumentado de navegación con mando.
- Empaquetado: permite generar una APK Fire TV solo `armeabi-v7a` con `-PfireAbiOnly=true` para reducir tamaño.
- Plex VOD: autentica mediante cabeceras las imágenes protegidas de DRBEP para recuperar los pósteres en la app offline sin exponer el token en la URL.
- Control remoto: la Web App puede enviar el canal en directo al Fire TV o dispositivo offline asociado al mismo usuario.
- Movistar HLS: `OK` abre los controles inferiores en directo en vez de pausar la imagen.
- Movistar HLS: reintenta en el proxy los fallos transitorios de segmentos antes de provocar un corte.
- Movistar HLS: muestra en el reproductor la resolución y el bitrate exactos declarados por la variante de máxima calidad.
- Plex AVI: reproduce los ficheros originales mediante libVLC local cuando Media3 no tolera sus timestamps, manteniendo direct play sin transcodificar ni remultiplexar.
- Plex AVI: abre la reanudación desde el punto guardado en el propio demuxer para evitar una espera larga antes del primer fotograma.
- Plex VOD: sitúa buscar y paginación encima de los resultados para acceder a ellos sin recorrer los 100 títulos.
- Ficha VOD: alinea el póster en formato 2:3 y separa título, metadatos, sinopsis, progreso y acciones en una composición más clara para TV.
- Navegación VOD: cierra la lista anterior antes de abrir búsqueda, ficha o reproducción para que el catálogo no quede superpuesto al vídeo.
- Reproduccion VOD: da prioridad al panel de canales sobre los controles de pelicula para que OK cambie al canal seleccionado.
- Plex VOD: añade navegación jerárquica por Películas o Series, servidor y biblioteca, con páginas cargadas bajo demanda.
- Plex Series: agrupa cada serie bajo un único título y muestra dentro sus episodios ordenados por temporada.
- Plex VOD: invalida la cache parseada anterior al actualizar la app y reconstruye el catalogo aunque la huella del snapshot no haya cambiado.
- Plex VOD: incorpora peliculas y series de los servidores configurados al catalogo offline y a la biblioteca visual.
- Plex VOD: reproduce siempre mediante la URL publica de DRBEP en direct play, sin transcodificacion ni remux.
- Plex VOD: conserva autenticacion de usuario, caratulas y filtros separados para peliculas y series.
- Reproduccion adaptativa: recuerda por canal la ruta compatible solo despues de 30 segundos estables y la descarta automaticamente si vuelve a fallar.
- VOD: muestra desde el primer instante un panel de preparacion con las fases de manifest, DRM y buffer en lugar de dejar la pantalla negra.
- Fire TV: la barra de avance consume izquierda y derecha tambien al alcanzar sus extremos, evitando que el foco salte al selector de plataforma.
- Tivify: restaura VOD Adulto en la carga bajo demanda y conserva tambien Runtime al abrir la biblioteca ligera.
- Movistar VOD: sustituye la copia reducida anterior al cargar la API dinamica para no mostrar una coleccion Peliculas duplicada y vacia.
- VOD Movistar: carga una selección reciente por API al abrir la biblioteca y busca bajo demanda en todo el catálogo sin guardar miles de títulos en el dispositivo.
- VOD Movistar: mantiene un catálogo offline reducido como respaldo si el proveedor o la red no están disponibles.
- U7D Movistar: habilita Últimos 7 días también en los canales DASH de la plataforma Movistar, reutilizando el catálogo y la reproducción de Movistar ISM.
- U7D: muestra en el HUD y la ficha el evento seleccionado en lugar del programa lineal actual, incluso despues de avanzar o retroceder.
- U7D Movistar: amplia dos minutos el final de la ventana para no cortar peliculas o programas que terminan despues del horario EPG.
- U7D Movistar y Orange: agrupa las pulsaciones consecutivas de avance/retroceso y abre un unico stream en el ultimo punto elegido.
- Fire TV: conserva el foco en la barra de tiempo mientras se desplaza con el mando y evita saltar al menu inferior durante el reinicio del U7D.
- Backend U7D: reemplaza la sesion anterior del mismo proveedor y dispositivo para que dos saltos no mantengan procesos ffmpeg compitiendo.
- Orange TV: corrige el tipo MPEG-TS de los replays U7D, habilita el retroceso virtual de dos horas y usa el logo del canal cuando falla la caratula del programa.
- Identidad visual: respeta la zona segura del icono adaptativo para mostrar completo el logo y sus textos sin perder el fondo negro a sangre.
- Identidad visual: el icono Android ocupa toda la mascara del launcher sin placa blanca y el nombre visible pasa a ser DRBEP TV.
- Identidad visual: incorpora el nuevo logo DRBEP en el icono de Android/Fire TV y en el banner del launcher.
- Orange TV: fija el MPD a su representacion de video de maxima calidad y recupera automaticamente los saltos de ventana live sin dejar una pantalla de error.
- Recuperacion offline: permite renovar una activacion caducada directamente desde la pantalla de problemas de actualizacion, sin borrar catalogo ni ajustes.
- Actualizacion offline: comprueba la APK en un executor prioritario antes de materializar el catalogo.
- Actualizacion obligatoria: mantiene bloqueada la carga pesada del catalogo si el servidor exige instalar una version nueva.
- Arranque: libera el catalogo tras cuatro segundos si el servidor de actualizaciones no responde, conservando un inicio tolerante a fallos.
- Arranque offline: usa primero la cache local firmada y vigente, sin consultar la huella remota en el camino critico.
- Fire TV: prioriza la reproduccion y difiere 25 segundos la hidratacion completa de VOD tras el primer frame, en un executor separado.
- Actualizacion offline: descarga y parsea el nuevo catalogo como candidato antes de sustituir atomicamente el ultimo snapshot valido.
- Fast playback: conserva el ultimo canal verificado ante cambios ajenos de catalogo o identificadores de navegacion.
- Tablet: espera 8 segundos de buffer antes de reanudar tras un corte para absorber mejor la irregularidad de los streams HLS.
- Telemetria: separa el buffering inicial de los rebufferings y reporta cada recuperacion sin inflar el tiempo ni los exitos de arranque.
- Diagnostico: sustituye la expresion regular de secretos por un redactor lineal y acotado para evitar ANR con URLs largas de Pluto TV.
- Seguridad: las URLs de diagnostico eliminan query y fragmento, y todo texto de entrada queda limitado antes de mostrarse en el HUD.
- Zapping: evita el ANR de Fire TV separando los cambios de calidad de la reconstruccion completa del listado y agrupando renders repetidos del overlay.
- Rendimiento: no regenera los 139 modelos de canal mientras el overlay esta oculto y reduce las actualizaciones duplicadas de seleccion/scroll.
- Estabilidad: cancela ejecutores y publicaciones UI de actividades destruidas, evitando tareas huerfanas, CPU alta y tormentas de GC tras recreaciones.
- Reproduccion: adelanta el offset fast-zap a la llegada de la linea temporal y evita levantar el servidor Smooth cuando la ruta final es HLS.
- Arranque: agrupa cargas EPG visibles duplicadas y activa la integracion generada de Glide.
- Playback: calidad Automatica/Ahorro/Alta; Automatica vuelve a usar seleccion adaptativa y Ahorro limita a 720p.
- Multiview: limita streams segun memoria del dispositivo y usa un perfil 540p/1.8 Mbps en tiles.
- Memoria: elimina `largeHeap`, reduce precarga en equipos de poca RAM y libera caches/multiview bajo presion.
- Accesibilidad: incorpora selector de subtitulos, targets adaptativos y foco visual diferenciado en acciones compartidas.
- Compose: conserva composiciones en HUD, overlays, grabaciones y multiview al actualizar sus modelos.
- Compatibilidad: activa desugaring para API 23, corrige usos API 24 y deja lint release sin errores.
- Build: migra al Kotlin integrado de AGP 9, activa cache/configuration cache y endurece CI con tests, lint vital y release.

## 2.0.284-beta-overlay-compose-surface
- Offline beta: unifica el overlay lateral en un modelo/superficie logica comun para cabecera, controles y lista.
- Offline beta: usa el estado central de overlays para decidir si timeshift debe ocultarse ante paneles bloqueantes.
- Offline beta: anade componentes Compose compartidos para chips/acciones de TV y los aplica al overlay.
- Offline beta: enruta el panel de grabaciones por el renderer Compose comun para preparar la siguiente migracion.

## 2.0.283-beta-compose-overlay-foundation
- Offline beta: introduce estado central de superficies de overlay para preparar una migracion Compose mas ordenada.
- Offline beta: enruta los binders principales del HUD/overlay por una capa comun de render Compose.
- Offline beta: extrae y testea la navegacion de foco del overlay lateral para proteger el movimiento con mando.
- Offline beta: anade tokens visuales compartidos para empezar a unificar color, foco y radios del HUD.

## 2.0.282-beta-playback-observability
- Offline beta: blinda los zaps y replays para que respuestas antiguas de stream info no puedan pisar la reproduccion actual.
- Offline beta: extrae y testea la politica de resolucion previa de stream info para Orange, Movistar ISM y DRM directo.
- Offline beta: anade fase, tiempos, bufferings y primer frame al diagnostico local, remoto y heartbeats de reproduccion.

## 2.0.261-beta-touch-epg-hydrate
- Offline beta: el HUD tactil hidrata el EPG usando la misma carga por lotes que el arranque de Fire TV, con fallback individual si hace falta.
- Offline beta: anade trazas visibles de hidratacion EPG tactil para diagnosticar telefono/tablet.

## 2.0.260-beta-startup-epg-trace
- Offline beta: asegura que la carga EPG prioritaria del canal actual se programe al aplicar el catalogo inicial.
- Offline beta: eleva las trazas de EPG de arranque a nivel visible en Fire TV para diagnosticar telefono/Fire sin ruido del sistema.

## 2.0.259-beta-priority-epg-load
- Offline beta: carga EPG del canal actual casi al arrancar y adelanta la carga del grupo visible para evitar quedarse sin guia en Movistar ISM.
- Offline beta: anade trazas claras de programacion, arranque y resultado de la carga EPG para diagnosticar movil/Fire TV.

## 2.0.258-beta-remote-epg-fallback
- Offline beta: el fallback EPG remoto evita volver a la EPG local cuando el snapshot no trae un canal concreto.
- Offline beta: Movistar ISM recupera programa actual/siguiente desde endpoints remotos autenticados si la EPG local no tiene ese canal.

## 2.0.257-beta-epg-u7d-fallback
- Offline beta: si el snapshot EPG local no trae programas para un canal concreto, la app vuelve a consultar el EPG remoto autenticado.
- Offline beta: U7D de Movistar ISM envia identificadores del canal al backend para recuperar programas aunque el informe global no matchee por nombre.

## 2.0.256-beta-vod-compose-polish
- Offline beta: la vista lista/densa de VOD pagina las secciones grandes para trabajar mejor con bibliotecas Movistar extensas.
- Offline beta: optimiza las caratulas VOD en carruseles y listas usando miniaturas mas ligeras, manteniendo calidad alta en la ficha.
- Offline beta: mejora el foco inicial y el comportamiento de Atras en biblioteca visual, fichas VOD, acciones VOD y listas rapidas.

## 2.0.255-beta-rich-hud-spacing
- Offline beta: sube la barra de timeshift para que no se pise con el HUD inferior enriquecido.
- Offline beta: estrecha la imagen EPG del HUD inferior para reducir espacio vacio.

## 2.0.254-beta-rich-touch-hud
- Offline beta: el HUD inferior muestra logo, canal, programa actual, siguiente programa, progreso e imagen del programa cuando hay EPG.
- Offline beta: hidrata la EPG del canal activo al abrir el HUD si aun no estaba cargada, sin bloquear el zapping.

## 2.0.253-beta-ism-timeshift-focus
- Offline beta: conserva el foco en la barra de timeshift al retroceder/avanzar varias veces con el mando.
- Backend: Movistar ISM deja de recortar el HLS a 30 segmentos y expone hasta 3600 segmentos (~2h) cuando el manifest original los trae.

## 2.0.252-beta-u7d-raw-time-tv-hud-seek
- Offline beta: U7D de Movistar ISM conserva `start_time/end_time` sin escapar para evitar 403 del CDN al servir desde ffmpeg.
- Offline beta: el HUD inferior de Fire TV deja de mostrar el boton de giro, sube para no quedar cortado y permite saltar a la barra de timeshift con el mando.
- Backend: limita los refresh automaticos de token U7D para que varios 403 simultaneos no pisen `u7d_report.json` ni disparen 429.

## 2.0.247-beta-u7d-stream-host-fix
- Offline beta: la reproduccion U7D de Movistar ISM usa el host principal cuando fire.tvbep.com no expone el endpoint de stream.
- Offline beta: las URLs de stream U7D adjuntan access_token y device_id como el resto de rutas offline protegidas.

## 2.0.246-beta-u7d-host-fallback
- Offline beta: U7D de Movistar ISM prueba el host offline y cae automaticamente a iptv.bepllorens.com si fire.tvbep.com no expone la ruta.
- Offline beta: anade logs con host y codigo HTTP del listado U7D para diagnosticar rapido problemas de rutas.

## 2.0.245-beta-u7d-offline-route-hud-width
- Offline beta: U7D en Movistar ISM lista programas desde una ruta offline autenticada, sin depender de la sesion del dashboard.
- Offline beta: envia token y device_id tambien al listado U7D para alinear permisos con stream/catalogo.
- Offline beta: ensancha el HUD inferior de Fire TV para que los botones no corten el texto al anadir U7D.

## 2.0.244-beta-u7d-backend-fire-hud
- Offline beta: anade el boton U7D al HUD inferior de Fire TV para canales Movistar ISM.
- Offline beta: carga los programas U7D desde el informe del backend Movistar, con fallback al EPG local.
- Offline beta: mejora el emparejamiento de canales U7D por nombre/codigo para casos como La 1 / TVE / HD / UHD.

## 2.0.243-beta-movistar-u7d
- Offline beta: anade acceso U7D en el HUD tactil para canales Movistar ISM, mostrando los ultimos programas disponibles del canal.
- Offline beta: reproduce U7D mediante endpoint seguro del backend sin transportar keys DRM en catalogo ni en la app.
- Offline beta: las URLs U7D protegidas adjuntan `access_token` y `device_id` igual que el resto de streams offline.

## 2.0.209-beta-catalog-fingerprint-startup
- Offline beta: valida una huella ligera del catalogo en el arranque y usa el snapshot local si no hay cambios, evitando descargar/parsing innecesario.
- Offline beta: muestra y reporta el ultimo arranque con cache validada para comprobarlo desde app y dashboard.
- Offline beta: mantiene compatibilidad si el backend antiguo no expone `/api/offline/snapshot/meta`, usando el snapshot local como fallback.

## 2.0.208-beta-vodafone-cdntv-cleartext
- Offline beta: permite HTTP para los subdominios `cdntv.vodafone.es` a los que redirige el MPD de `La 1 UHD`.

## 2.0.207-beta-vodafone-cleartext-epg
- Offline beta: permite HTTP solo para los dominios Vodafone usados por `La 1 UHD` en el MPD local.
- Offline beta: conserva los reintentos de MPD y la hidratacion diferida de EPG del catalogo completo.

## 2.0.206-beta-dash-epg-hydration
- Offline beta: `La 1 UHD` reintenta la descarga del MPD local y puede servir el ultimo manifest bueno si Vodafone corta la conexion.
- Offline beta: carga EPG del grupo visible y despues hidrata el resto del catalogo en diferido.

## 2.0.205-beta-remote-epg-guard
- Offline beta: evita abrir snapshots grandes para decidir si hay EPG local.
- Offline beta: si el catalogo ligero no trae EPG local, usa el EPG remoto autenticado para la guia actual.

## 2.0.204-beta-deferred-visible-epg
- Offline beta: recupera la carga diferida de EPG actual para los canales visibles tras pintar el catalogo.
- Offline beta: mantiene bloqueada la precarga EPG completa al arrancar para evitar volver al arranque pesado.

## 2.0.203-beta-startup-lite-epg-guard
- Offline beta: permite guardar el catalogo ligero de arranque sin rechazarlo como reduccion sospechosa.
- Offline beta: evita precargar EPG completo tras el primer frame cuando el catalogo ligero no trae programas.

## 2.0.202-beta-startup-lite-catalog
- Offline beta: si el snapshot local es demasiado grande, refresca un catalogo ligero de arranque antes de parsearlo.
- Offline beta: evita que EPG/VOD pesado bloquee el primer pintado de canales en Fire Stick.

## 2.0.201-beta-startup-unblock
- Offline beta: pinta la lista de canales antes de iniciar autoplay para evitar arranques aparentes sin canales.
- Offline beta: retrasa el autoplay inicial en Fire Stick y evita que Movistar ISM active el proxy Smooth local.

## 2.0.200-beta-movistar-ism-hls
- Offline beta: restaura Movistar ISM usando la ruta HLS fMP4 `/hls/ism/{id}/index.m3u8`.
- Offline beta: evita que los canales Smooth ClearKey de Movistar ISM caigan en el manifiesto Smooth local del Fire Stick.

## 2.0.199-beta-drm-device-token
- Offline beta: anade `access_token` y `device_id` en URLs sensibles de reproduccion y licencias DRM.
- Offline beta: evita fallos tras el endurecimiento del backend cuando el callback DRM no conserva todos los headers.

## 2.0.198-beta-secure-drm-hotfix
- Offline beta: corrige el cifrado AES-GCM del snapshot dejando que Android Keystore genere el IV.
- Offline beta: recupera Movistar ISM priorizando la licencia ClearKey local resuelta desde `/api/stream`.

## 2.0.197-beta-secure-drm-control
- Offline beta: prepara el control plane seguro para DRM resolviendo claves ClearKey bajo demanda por `/api/stream`.
- Offline beta: el export de catalogo sustituye secretos DRM embebidos por `drm_ref` por defecto.
- Offline beta: VOD con DRM usa licencia remota en vez de transportar ClearKeys dentro del snapshot local.

## 2.0.196-beta-security-hardening
- Offline beta: endurece la build release desactivando backup Android y cleartext global.
- Offline beta: cifra el snapshot local del catalogo con AES-GCM y clave de Android Keystore, migrando catalogos antiguos al leerlos.
- Offline beta: elimina fallbacks LAN/dev de EPG, updates y clasificacion de trafico en la app publica.

## 2.0.195-beta-compose-playback-split
- Offline beta: playback separa menus de modo permanente/temporal en una factory dedicada.
- Offline beta: la resolucion de modo efectivo y el auto-repair/learned routes pasan a coordinadores separados.
- Offline beta: el panel de recovery VOD se extrae de `MainActivity` manteniendo retry, diagnostico y biblioteca.

## 2.0.194-beta-compose-ui-split-5
- Offline beta: VOD avanzado separa categorias y acciones de progreso en factories reutilizables.
- Offline beta: update, recordings, timeline y listas rapidas extraen mas construccion visual fuera de `MainActivity`.
- Offline beta: se avanza en los cinco bloques pendientes dejando para el final la orquestacion profunda de playback/rutas.

## 2.0.193-beta-compose-menu-factories
- Offline beta: diagnostico de reproduccion separa panel principal, historial y acciones avanzadas en una factory dedicada.
- Offline beta: listas personales, ficha de canal, ajustes, herramientas y menu VOD clasico pasan a factories de UI reutilizables.
- Offline beta: se reduce mas peso de `MainActivity` manteniendo la navegacion de vuelta entre submenus y el comportamiento existente.

## 2.0.192-beta-compose-vod-settings
- Offline beta: VOD visual separa el ensamblado de panel, filtros, secciones y tarjetas en una factory dedicada.
- Offline beta: Ajustes empieza a separar los menus de centro, inicio, busqueda y reproduccion en una factory de UI.
- Offline beta: las acciones del canal actual pasan a una factory dedicada, manteniendo orden y comportamiento.

## 2.0.191-beta-compose-timeline-timeshift
- Offline beta: la guia timeline separa la construccion de filas y bloques en una factory dedicada, conservando foco, acciones y grabacion directa.
- Offline beta: el HUD de timeshift separa el modelo y callbacks de seek/live en una factory dedicada.
- Offline beta: se avanza en la reduccion de logica UI dentro de `MainActivity` sin cambiar comportamiento visible.

## 2.0.190-beta-compose-dialog-core
- Offline beta: los paneles nucleares de opciones e introduccion de texto pasan tambien por el host comun de dialogos Compose fullscreen.
- Offline beta: desaparecen los dialogos fullscreen legacy manuales de `MainActivity`, dejando una ruta comun para foco, cancelacion y cierre modal.
- Offline beta: la gestion visual de listas personales separa la construccion de filas en una factory dedicada.

## 2.0.189-beta-compose-factory-dialogs
- Offline beta: busqueda global, resultados EPG, mini guia y banner inferior de zapping separan la construccion de modelos UI en factories dedicadas.
- Offline beta: se introduce un host comun para dialogos Compose fullscreen y se aplica a VOD, EPG visual, timeline, busqueda, listas, activacion offline y paneles de diagnostico/mensaje.
- Offline beta: la barra tactil en movil/tablet ajusta los chips con ancho minimo y texto centrado para reducir cortes y saltos visuales.

## 2.0.188-stable-compose-base
- Offline stable: se consolida como base estable la tanda probada de tablet/movil, HUD tactil, multiview, Orange, catalogo offline y paneles Compose.
- Offline stable: incluye el cambio de plataforma/grupo en tablet con sintonizacion automatica del primer canal visible.
- Offline stable: mantiene la separacion progresiva de modelos UI fuera de `MainActivity` para seguir el desarrollo en beta desde esta base.

## 2.0.187-beta-compose-recordings-polish
- Offline beta: el cambio de plataforma/grupo en tablet sintoniza automaticamente el primer canal visible cuando el canal actual no pertenece al nuevo filtro.
- Offline beta: la barra tactil muestra mejor el contexto activo de plataforma/grupo y el multiview mejora sus etiquetas de ventana y audio activo.
- Offline beta: el panel de grabaciones separa la construccion de modelos UI en una factory dedicada, reduciendo acoplamiento de `MainActivity` dentro de la migracion a Compose.

## 2.0.186-beta-catalog-oom-guard
- Offline beta: se evita un cierre por falta de memoria al actualizar catalogos con campos URL/texto anormalmente grandes, descartandolos antes de normalizar el snapshot.
- Offline beta: se blindan IDs, grupos, filtros VOD y reglas parentales para que un campo corrupto del catalogo no pueda tumbar el arranque de Fire TV.

## 2.0.185-beta-orange-startup-fix
- Offline beta: Orange resuelve `StreamInfo` antes de preparar el player para evitar el doble arranque/replay que podia dejar pantalla negra o hacer el zapping mucho mas lento.
- Offline beta: se retira el override manual tardio de pista maxima en Orange; el selector sigue prefiriendo la mejor calidad soportada, pero sin forzar un cambio de track despues de arrancar.
- Offline beta: los logs de reproduccion incluyen tiempos hasta `READY` y primer frame para diagnosticar si el retraso viene de manifest, licencia, decoder o render.

## 2.0.184-beta-orange-fastzap
- Offline beta: Orange fuerza la pista de video soportada de mayor calidad cuando Media3 expone varias variantes, evitando quedarse en perfiles inferiores si el dispositivo puede con la senal.
- Offline beta: el zapping reduce buffers iniciales y evita resolver rutas no directas antes de preparar el player, recortando esperas en Fire TV sin cambiar los fallbacks de compatibilidad.
- Offline beta: se eliminan avisos superiores ruidosos durante `buffering`, `ready`, rutas directas, Widevine y zapping para que el HUD inferior sea la referencia visual principal.
- Offline beta: los filtros internos tipo `Todos`/`all` dejan de aparecer como destino de navegacion cuando hay filtros reales de plataforma/grupo.
- Offline beta: los paneles VOD, EPG visual y detalles de programa conservan mejor el retorno al menu/panel anterior al cerrar o al pasar por desbloqueo parental.

## 2.0.177-beta-compose-structured-panels
- Offline beta: diagnostico de playback e historial de fallos pasan a paneles Compose estructurados con secciones, estados de ruta/error y acciones con foco inicial.
- Offline beta: los avisos de actualizacion, diagnostico de actualizacion y pantalla post-update muestran version, canal, APK, preflight y changelog en formato estructurado.
- Offline beta: los resumenes de ajustes/sistema/catalogo/familia se convierten automaticamente en filas Compose cuando vienen como `Clave: valor`, manteniendo fallback para texto libre.

## 2.0.176-beta-live-ready-overlay
- Offline beta: los canales live con reproduccion directa, como Movistar ISM u Orange, dejan de mostrar el aviso superior `VOD listo` al terminar de preparar el zapping.
- Offline beta: la peticion interna de reproduccion separa ahora `VOD` de `directPlayback`, evitando confundir canales directos con contenido bajo demanda.

## 2.0.141-beta-la1uhd-template-escape
- Offline beta: el MPD local de `La 1 UHD` escapa el `$` de la ruta `LIVE$CUP...` al absolutizar segmentos, evitando que ExoPlayer lo confunda con una variable DASH invalida.

## 2.0.140-beta-la1uhd-patched-target
- Offline beta: el resolver da prioridad al MPD local parcheado en directos DASH ClearKey, evitando que `La 1 UHD` vuelva a cargar el manifest original sin `default_KID`.

## 2.0.139-beta-la1uhd-direct-resolve
- Offline beta: los canales directos DASH ClearKey, como `La 1 UHD`, pasan por resolucion previa de StreamInfo para activar el manifest local parcheado antes de reproducir.

## 2.0.138-beta-la1uhd-clearkey-direct
- Offline beta: `La 1 UHD` parchea localmente el MPD directo de Vodafone con `cenc:default_KID`, evitando pantalla negra por manifests ClearKey sin KID.
- Offline beta: `La 1 UHD` deja de arrancar con cap fijo de compatibilidad, permitiendo seleccionar la pista HEVC 2160p en Fire TV 4K compatibles.

## 2.0.137-beta-tdt-uhd-overlay-context
- Offline beta: el overlay lateral muestra en la cabecera el contexto actual de plataforma o grupo, por ejemplo `TDT` o `Vodafone`, para saber donde estas mientras navegas canales.
- Offline backend: `La 1 UHD` y otros DASH ClearKey de `vfsmartcdn.gb.vodafone.es` se exportan como reproduccion directa, evitando el perfil proxy que no arrancaba en la app offline.

## 2.0.136-beta-mobile-relaunch-catalog-baseline
- Offline beta: se evita el cierre en moviles cuando Android recrea la Activity por orientacion durante el arranque y quedaban callbacks intentando usar un executor ya cerrado.
- Offline beta: la proteccion de catalogo reducido ya no bloquea un snapshot firmado cuando solo baja live pero el total del catalogo sigue dentro de margen, permitiendo adoptar el nuevo baseline bueno del servidor.

## 2.0.135-beta-mobile-platform-picker
- Offline beta: la barra tactil inferior incorpora un boton `Plataforma` que abre un selector nativo con todos los filtros/plataformas/grupos disponibles.
- Offline beta: elegir una plataforma/grupo desde ese selector refresca la lista de canales y mantiene visible el overlay, sin depender de flechas ocultas ni cabeceras Compose experimentales.

## 2.0.134-beta-mobile-filter-header-rollback
- Offline beta: se desactiva temporalmente la cabecera tactil de filtros de la lista movil para evitar el cierre al iniciar detectado en la beta anterior.
- Offline beta: se conserva el copy mas claro de `Canales`, `Plataforma / grupo`, `Ant.` y `Sig.` mientras se revisa la cabecera con log del Moto.

## 2.0.133-beta-mobile-filter-header
- Offline beta: la lista de canales en movil incluye una cabecera fija `Plataforma / grupo` con botones grandes `◀` y `▶` para cambiar de plataforma/grupo sin buscar controles ocultos.
- Offline beta: el scroll de la lista compensa la nueva cabecera para mantener seleccionado el canal correcto.

## 2.0.132-beta-mobile-platform-filter-ux
- Offline beta: en moviles el boton principal pasa a llamarse `Canales` y el selector superior de la lista explica que cambia entre plataforma/grupo.
- Offline beta: los controles tactiles de filtro dejan de mostrarse como flechas sueltas y pasan a `Ant.` / `Sig.` con etiquetas mas cortas para evitar cortes en pantallas estrechas.

## 2.0.131-beta-mobile-startup-crash-fix
- Offline beta: se evita el cierre al arrancar en moviles cuando un callback tardio de catalogo/reproduccion coincide con el cambio de orientacion inicial.
- Offline beta: la reproduccion inicial revalida que la Activity y el `PlayerController` siguen activos antes de preparar el canal.

## 2.0.130-beta-mobile-tv-all-filter
- Offline beta: el catalogo standalone vuelve a exponer el filtro `Todos`, evitando que moviles/tablets queden aparentemente limitados a la primera plataforma disponible.
- Offline beta: el acceso rapido `TV` del hub tactil prioriza `Todos` cuando existe, mientras la lista principal evita mezclar VOD en ese filtro.

## 2.0.129-beta-compose-vod-epg
- Offline beta: la EPG visual avanza en Compose con cabecera, detalle, secciones y tarjetas de programa, manteniendo la navegacion con mando existente.
- Offline beta: la ficha VOD y el panel de acciones VOD migran sus bloques principales a Compose para un layout mas consistente en Fire TV, moviles y tablets.
- Offline beta: se unifica la semantica de foco de botones Compose para mejorar accesibilidad y navegacion con mando en los nuevos paneles.

## 2.0.128-beta-pluto-proxy-hls-token
- Offline beta: Pluto vuelve a tratarse como HLS sobre `proxy_manifest`, evitando que ExoPlayer intente abrir esos manifests como DASH cuando aun no hay `StreamInfo`.
- Offline beta: las rutas media servidas desde `fire.tvbep.com` reciben tambien el `access_token` por query para que las playlists hijas y segmentos del proxy HLS no acaben devolviendo `session required`.

## 2.0.127-beta-pluto-runtime-direct-label
- Offline beta: Pluto deja de publicarse en el snapshot como `proxy_manifest`; los HLS sin DRM de Pluto se entregan como reproduccion directa para evitar caidas a modo compatibilidad.
- Offline beta: Runtime live/VOD se etiqueta como `Directo HLS` y no como trafico servidor cuando el servidor solo resuelve el manifest y las playlists/segmentos ya salen hacia CDN externa.

## 2.0.126-beta-runtime-vod-direct-menu
- Offline beta: `Actualizar catalogo local` vuelve a estar visible directamente en `Herramientas` y en el hub de arranque, para refrescar el snapshot sin entrar en ajustes profundos.
- Offline beta: las rutas directas resueltas desde `StreamInfo.sourceUrl` ya no se fuerzan a proxy solo porque la URL inicial del catalogo venga de `/live/{id}` del backend.
- Servidor: el VOD de Runtime en snapshots offline se publica con base publica principal para evitar 404 en `fire.tvbep.com` cuando la app esta fuera de casa.

## 2.0.124-beta-movistar-compat-restore
- Offline beta: Movistar normal vuelve a priorizar la ruta compatible/proxy en standalone cuando el DASH ClearKey directo esta devolviendo `Source error`, evitando que el usuario vea intentos fallidos antes de caer a reproduccion compatible.
- Offline beta: se mantiene la correccion de telemetria para que el dashboard no se quede pegado en `trafico servidor` si la ruta final activa ya es directa de verdad.

## 2.0.123-beta-direct-traffic-telemetry-fix
- Offline beta: el heartbeat deja de marcar como `trafico servidor` reproducciones DRM/HLS/DASH que ya van directas al origen solo por arrastrar un `proxy_manifest` historico.
- Offline beta: las rutas finales directas vuelven a etiquetarse como `Directo HLS`, `Directo DASH`, `Directo Smooth` o `Directo DRM` tambien cuando el modo `auto` resuelve un origen directo sin pasar por el backend.

## 2.0.122-beta-standalone-direct-route-balance
- Offline beta: el modo standalone deja de ser `proxy-first` tambien en HLS y DASH normales, no solo en algunos DRM concretos de Movistar.
- Offline beta: los modos `proxy` aprendidos automaticamente ya no fuerzan proximos arranques en standalone, permitiendo reevaluar rutas directas tras instalar la nueva beta.
- Offline beta: los canales/plataformas Adultos o Hot mantienen una ruta conservadora para no romper su reproduccion mientras afinamos su compatibilidad aparte.

## 2.0.121-beta-drm-direct-first-offline
- Offline beta: el modo `auto` del reproductor independiente pasa a priorizar rutas directas tambien en muchos canales DRM, y solo cae a proxy cuando la reproduccion directa falla.
- Offline beta: los perfiles `proxy_manifest` dejan de bloquear por completo las rutas directas en standalone, permitiendo que el aprendizaje automatico favorezca ahorro real de trafico del servidor.

## 2.0.120-beta-actual-route-directness-fix
- Offline beta: el heartbeat ya no considera `direct_playback` real solo porque el catalogo marque el canal como directo; ahora se alinea con el modo y la ruta final que usa el reproductor.
- Offline beta: esto evita diagnosticos confusos en sesiones que empiezan como candidatas a directo pero acaban resolviendose por `Proxy DRM`.

## 2.0.119-beta-direct-playback-heartbeat-fix
- Offline beta: el heartbeat deja de enviar `direct_playback` solo desde el catalogo y pasa a reportar la ruta real que ha resuelto el reproductor.
- Offline beta: se conserva `catalog_direct_playback` como dato auxiliar para distinguir lo que decia el snapshot frente a lo que realmente se esta usando en reproduccion.

## 2.0.118-beta-direct-traffic-heartbeat-fix
- Offline beta: los heartbeats de reproduccion dejan de marcar como trafico de servidor cualquier stream directo solo por parecer `hls/live` o por colgar del mismo host.
- Offline beta: ahora pesa primero el modo real de reproduccion directa y solo se reporta trafico de servidor en rutas backend claras como `proxy`, `remux` o `recordings`.

## 2.0.117-beta-mobile-launcher-manifest-fix
- Offline beta: la app deja de declarar orientacion fija en el `manifest`, evitando que algunos launchers Android la traten como app solo-paisaje/TV y no la muestren bien en moviles Motorola.
- Offline beta: el modo tactil sigue forzando paisaje desde codigo, asi que el comportamiento visual de la app no cambia aunque el icono ya pueda quedar normal en el launcher.

## 2.0.116-beta-device-label-mobile-visibility
- Offline beta: la activacion por codigo ya registra el nombre real del dispositivo, por ejemplo un movil Motorola, en lugar de etiquetarlo siempre como `Fire Stick offline`.
- Offline beta: los reportes al servidor incluyen tambien fabricante, modelo y tipo de dispositivo para que el dashboard nuevo pueda distinguir mejor moviles, tablets y TV.

## 2.0.115-beta-mobile-launcher-hub-flow
- Offline beta: la app ya se expone como lanzador Android normal en moviles y tablets, sin depender de abrirla manualmente por ADB.
- Offline beta: el hub tactil se reparte en varias filas en pantallas estrechas, evitando botones demasiado comprimidos y mejorando el uso real en telefono.

## 2.0.114-beta-guide-dialog-owner-fix
- Offline beta: `Guia` deja de cerrar la app en Fire TV al abrir el timeline fullscreen con Compose, propagando correctamente el ciclo de vida al dialogo.
- Offline beta: el mismo refuerzo se aplica a la vista fullscreen de EPG visual para evitar el mismo patron de crash en paneles similares.

## 2.0.113-beta-mobile-touch-crash-fix
- Offline beta: la app deja de cerrarse al arrancar en moviles Android por una medicion invalida del panel tactil horizontal.
- Offline beta: la barra tactil conserva el scroll horizontal, pero ya solo en la capa nativa para evitar conflictos de Compose en pantallas de telefono.
- Offline beta: el HUD principal gana algo mas de ancho util y ajusta mejor el tamano del texto para evitar etiquetas truncadas como `Canales`.

## 2.0.112-beta-hud-actions-nav-fix
- Offline beta: el HUD de TV recupera todas las acciones rapidas en una sola fila realmente ancha, con reparto equilibrado entre botones.
- Offline beta: la seleccion del HUD se vuelve a pintar al instante al mover el mando, y anade accesos directos a calidad y favorito del canal actual.

## 2.0.111-beta-hud-nav-header-fix
- Offline beta: el HUD de TV vuelve a una sola fila mas ancha y la seleccion lateral deja de hacer saltos raros al llegar a los extremos.
- Offline beta: el overlay lateral recupera la cabecera informativa de plataforma/canal, pero sigue reservando mucho mas espacio real para la lista.

## 2.0.110-beta-overlay-hud-space-fix
- Offline beta: el HUD inferior reparte las acciones en filas mas estables para que no se recorten ni queden tan apretadas.
- Offline beta: el overlay de canales en TV prioriza la lista y esconde el panel superior de exploracion/estado que quitaba espacio sin aportar navegacion real.

## 2.0.109-beta-compose-timeline-grid-refactor
- Offline beta: la guia Timeline avanza bastante en su migracion a Compose con cabecera, escala horaria, placa de canal, bloques visuales de programa y estado sin EPG.
- Offline beta: la rejilla timeline se refactoriza por dentro en filas, strips y bloques visibles para facilitar las siguientes correcciones sin tocar la navegacion con mando.

## 2.0.108-beta-compose-timeline-shell
- Offline beta: varios paneles del modo offline pasan a Compose, incluyendo busqueda global, mini guia, resultados EPG, listas personales y canales rapidos.
- Offline beta: la guia Timeline empieza su migracion a Compose con la cabecera y la ficha inferior de detalle, manteniendo intacta la navegacion fina con mando en la rejilla principal.

## 2.0.107-beta-tv-hud-centered-actions
- Offline beta: la fila de acciones del HUD se centra mejor dentro del panel para que `Canales` no roce el borde.
- Offline beta: la pastilla de calidad se compacta un poco para dejar mas aire en la cabecera.

## 2.0.106-beta-tv-hud-order-spacing
- Offline beta: el HUD baja un poco mas en pantalla y el primer boton deja de recortarse.
- Offline beta: los dos primeros accesos pasan a `Canales` y `Guia`, manteniendo el mismo orden tambien con el mando.

## 2.0.105-beta-tv-hud-smaller-logo-fill
- Offline beta: el HUD inferior se compacta aun mas para ocupar menos pantalla.
- Offline beta: el logo del canal rellena mejor la placa y gana tamaño visual sin cambiar el marco.

## 2.0.104-beta-tv-hud-compact-logo
- Offline beta: el HUD inferior reduce un poco mas su tamaño general para encajar mejor en TV.
- Offline beta: el logo del canal gana presencia dentro de su marco sin agrandar la placa exterior.

## 2.0.103-beta-tv-hud-nav-fix
- Offline beta: el HUD inferior reduce tamaño y densidad para verse mejor en TV y no quedar desproporcionado.
- Offline beta: los accesos del HUD ya se recorren con izquierda/derecha y se activan con OK desde el mando.

## 2.0.102-beta-tv-hud-backstack
- Offline beta: el cartel inferior de zapping pasa a un HUD mas rico con logo, programa actual, siguiente, progreso y accesos rapidos a guia, canales, grabar, familia, audio y mas.
- Offline beta: la tecla `Atras` cierra primero el HUD inferior y los submenus de ajustes/familia vuelven al nivel anterior en vez de expulsarte de todo el flujo.

## 2.0.101-beta-epg-channel-mapping
- Offline beta: la EPG remota y la embebida en snapshot ya se cruzan por `channel_id`, `tvg_id` y nombre de canal, evitando que solo casen plataformas como Pluto.
- Herramientas internas: el exportador de snapshots rellena la guia por canal con fallback desde `/api/epg/now` cuando el endpoint por `channel_id` no devuelve datos.

## 2.0.100-beta-epg-host-fallback
- Offline beta: el fallback de EPG remoto prueba varias bases compatibles y evita quedarse atascado en hosts que no publican `/api/epg`, como `fire.tvbep.com`.
- Offline beta: al encontrar una base EPG valida, la app la reutiliza para acelerar las siguientes consultas de guia.

## 2.0.99-beta-offline-epg-fallback
- Offline beta: la app vuelve a cargar EPG aunque el snapshot local no traiga la seccion `epg`, usando fallback autenticado al backend offline.
- Offline beta: las consultas EPG en modo independiente envian token y `device_id`, igual que el resto de flujos protegidos.
- Herramientas internas: el exportador manual de snapshots ya puede incrustar `epg` por canal para pruebas o despliegues externos.

## 2.0.95-beta-menu-back-parental
- Offline beta: los submenus del menu offline ya respetan `Atras` y vuelven al nivel anterior en lugar de cerrar todo el flujo de golpe.
- Offline beta: `Familia y control` y `Control parental` dejan de parecer solo un cartel informativo y muestran acciones claras para ver estado, configurar, cambiar, desbloquear o quitar el PIN.
- Offline beta: los menus con jerarquia usan `Volver` como salida contextual y reservan `Cerrar` para salir realmente del flujo.

## 2.0.94-beta-offline-menu-copy
- Offline beta: `Herramientas` vuelve a abrir el menu principal correctamente en lugar de quedarse solo en un texto descriptivo.
- Offline beta: el menu simple usa copywriting mas claro para usuarios no tecnicos, con etiquetas como `TV y guia`, `Peliculas y series`, `Familia y control` y `Opciones avanzadas`.
- Offline beta: varios accesos internos se renombran para que sean mas faciles de entender con mando, incluyendo grabaciones, diagnostico, listas y multiview.

## 2.0.93-beta-simple-offline-menu
- Offline beta: `Herramientas` pasa a un menu simple para uso diario con bloques claros de TV/guia, grabaciones, VOD, busqueda, familia y avanzado.
- Offline beta: el hub rapido y los accesos tactiles dejan mas a mano `Control parental`, guia y grabaciones, y esconden lo tecnico detras de `Ajustes avanzados`.
- Offline beta: el contenido protegido desbloqueado sigue marcado visualmente con candado en filtros, listas y accesos relevantes.

## 2.0.92-beta-parental-pin
- Offline beta: la app ya admite PIN local para contenido protegido por el dashboard offline, con desbloqueo temporal por sesion.
- Offline beta: bouquets, filtros y canales marcados por el snapshot se ocultan mientras la sesion siga bloqueada y piden PIN al intentar abrirlos.
- Dashboard: el snapshot offline puede marcar `parental_group_names`, `parental_channel_ids`, `parental_filter_keys` y `parental_vod_adult`.

## 2.0.91-beta-device-traffic-thumbnails
- Offline beta: los heartbeats de reproduccion envian programa actual, miniatura EPG/logo y si el stream esta consumiendo trafico del servidor.
- Servidor: Actividad offline estima consumo por sesion con el bitrate real detectado y separa el trafico que pasa por DRBEP.
- Dashboard: "Viendo ahora" muestra miniatura del programa o logo del canal junto con programa actual y consumo estimado.

## 2.0.90-beta-recordings-playback-auth
- Offline beta: la reproduccion de grabaciones anade el token de activacion para poder abrir remux desde el dominio publico.
- Servidor: el host publico offline permite las rutas de grabaciones necesarias sin exponer el resto de aplicaciones.
- Offline beta: si el usuario no tiene permiso para programar grabaciones, la app muestra un aviso claro.

## 2.0.89-beta-timeline-recording-shortcut
- Offline beta: la guia Timeline permite programar/cancelar una grabacion directamente con la tecla menu sobre un programa.
- Offline beta: las acciones de programa registran trazas claras para diagnosticar si el menu ejecuta grabar, cancelar o solo se abre.

## 2.0.88-beta-epg-recording-actions
- Offline beta: la programacion de grabaciones desde EPG visual/timeline ya no queda bloqueada por el modo independiente si hay servidor configurado.
- Offline beta: las llamadas de grabaciones usan el token de activacion offline para que el backend pueda autorizar la operacion.
- Offline beta: al programar desde la guia se muestra estado de progreso y se marca el programa como programado en las guias abiertas.

## 2.0.87-beta-epg-guide-stability
- Offline beta: la guia Timeline renderiza una ventana limitada de canales alrededor del canal actual para evitar cierres por ANR en Fire TV.
- Offline beta: la EPG visual limita las tarjetas iniciales por seccion y prioriza el canal seleccionado para abrir mas rapido en dispositivos Fire Stick.

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
## 2.0.125-beta-direct-routing-pluto-zap

- Corrige la deteccion de trafico directo cuando una CDN externa contiene `/live/` en su propia URL.
- Pluto arranca directamente desde el HLS del catalogo y evita la consulta previa que ralentizaba el zapping.
- Mantiene la ruta de compatibilidad de Pluto como recuperacion si falla el origen directo.
## 2.0.379-black-letterbox

- Fuerza a negro puro las bandas de relación de aspecto y el fondo de vídeo
  tanto en Media3 como en la reproducción directa con VLC.

## 2.0.378-vod-page-focus

- Permite saltar desde el último título de una página VOD a `Página siguiente`
  pulsando abajo, sin recorrer de nuevo los 100 resultados.
- Conserva el acceso directo a Buscar pulsando arriba desde el primer título.
- Añade ordenación del catálogo Plex por fecha real de incorporación o por
  título, con los añadidos recientemente como vista inicial.
## 2.0.380-movistar-sample-aes

- Movistar HLS: reproduce los canales MPEG-TS SAMPLE-AES importados desde JSON mediante el manifiesto identity autenticado y libVLC, sin transcodificación.
## 2.0.433-remote-messages

- Muestra avisos remotos del administrador sobre la reproducción en Fire TV, Android TV y dispositivos táctiles.
- Confirma entrega y lectura, evita mensajes duplicados y descarta avisos caducados.

## 2.0.434-visible-remote-messages

- Muestra los avisos remotos en una capa propia sobre el reproductor para que sean visibles también en Fire TV.
- Confirma por separado la entrega y la lectura del aviso desde el botón `Entendido`.
