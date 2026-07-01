# Offline beta release notes

Notas acumuladas para promocionar la app offline de beta a stable.

## Pendiente de promocionar a stable

Rango actual: `2.0.176-beta-live-ready-overlay`.

### Navegacion y catalogo

- Los filtros de grupo respetan el orden especifico guardado en el dashboard para cada grupo.
- El snapshot offline incluye `group_order`, evitando mezclar el orden de plataforma con el orden de grupo.
- La app adopta el canal de actualizaciones efectivo devuelto por el servidor, por ejemplo cuando un dispositivo se mueve de stable a beta desde el dashboard.

### Actualizaciones

- El instalador de actualizaciones usa `ACTION_INSTALL_PACKAGE` como ruta principal en Fire OS.
- La beta publicada actualmente es `2.0.176-beta-live-ready-overlay` (`versionCode 176`).
- Movistar ISM y Orange dejan de mostrar el aviso superior `VOD listo` al zapear canales live directos.
- En grabaciones programadas, la app descarta registros cerrados (`completed`, `failed`, `stopped`, `canceled`) y programaciones vencidas para no mostrar entradas antiguas borradas/obsoletas del dashboard.
- Las consultas de grabaciones usan cache-buster y cabeceras `no-cache`, de modo que `Refrescar` fuerza una lectura fresca del backend.
- En grabaciones, izquierda/derecha se gestionan desde el router del mando y mueven un foco superior propio entre `Completadas`, `Programadas` y `Refrescar`, evitando que izquierda cierre el panel o que derecha quede bloqueada.
- En grabaciones, los botones `Completadas`, `Programadas` y `Refrescar` recuperan foco visible y navegacion izquierda/derecha con el mando; `Back` sigue cerrando el panel.
- El panel lateral de grabaciones pasa a una unica superficie Compose completa, eliminando el contenedor XML y los binders separados de cabecera/lista.
- La lista rapida de canales usada para cambiar un canal del multiview enfoca automaticamente la primera fila y muestra cursor visual.
- La busqueda rapida elimina su wrapper XML y queda como superficie Compose directa.
- `MainActivity` elimina campos duplicados para zap/busqueda rapida y retira el codigo VOD visual legacy basado en `RecyclerView`.
- Se eliminan drawables y dimensiones XML que solo pertenecian a pantallas legacy ya migradas.
- Multiview conserva sus cuatro reproductores nativos, pero cabecera, cierre y overlays de cada tile pasan a Compose.
- El canal seleccionado en la lista lateral queda anclado mas arriba y la lista recibe padding inferior para evitar que el cursor quede en una fila cortada.
- El scroll lateral usa un token monotono por pulsacion y posiciona la ventana unos canales antes del seleccionado para que avance al llegar al borde inferior.
- La lista lateral conserva un modelo Compose estable para no reiniciar el `LazyListState` en cada pulsacion del mando.
- La lista lateral de canales Compose vuelve a capturar DPAD arriba/abajo y fuerza scroll animado al canal seleccionado.
- El overlay lateral mueve a Compose sus tarjetas principales, la etiqueta de lista y el buscador, eliminando el `EditText` XML y wrappers decorativos.
- El HUD de zapping, la barra timeshift y el hub tactil inferior eliminan carcasas XML intermedias y pasan a ser superficies Compose directas.
- El estado vacio de la lista lateral pasa a vivir dentro del componente Compose de canales y se retiran nodos XML muertos del overlay/root.
- El root principal empieza a perder XML decorativo: busqueda rapida, estado, HDR, LIVE/timeshift y errores pasan a componentes Compose.
- Se elimina el adapter/list item XML antiguo de canales, ya reemplazado por la lista lateral Compose.
- Busqueda global pasa a un panel Compose completo con campo de texto, filtros y resultados en el mismo arbol, eliminando `dialog_channel_search.xml`.
- Guia Timeline y EPG visual eliminan sus contenedores XML heredados y crean su estructura fullscreen desde codigo con componentes Compose vivos.
- La ficha VOD y el panel de acciones VOD pasan a un panel Compose unico con poster, metadatos, progreso y acciones focusables.
- Canales rapidos, canales de listas personales, resultados EPG y mini guia pasan a paneles fullscreen Compose reales con cabecera integrada, eliminando los layouts XML y adapters RecyclerView heredados de esos flujos.
- `MainActivity` y `ChannelActionsCoordinator` eliminan los `AlertDialog` restantes: acciones de canal/programa y codigo de activacion offline pasan a paneles Compose fullscreen.
- Se migra otro bloque grande de dialogos legacy a Compose: recuperacion de catalogo, continuar VOD/grabaciones, diagnosticos, historial, recuperacion VOD, aviso salir de grabacion, acerca de, busqueda VOD/EPG, modos playback, presets multiview, selector de filtros, listas personales de canal y canal de actualizaciones.
- Busqueda global, resultados EPG, mini guia, canales rapidos y canales de listas personales dejan de usar `AlertDialog` como contenedor y pasan a `Dialog` fullscreen con contenido Compose.
- `Organizar mis listas` mejora el foco con mando, mantiene `Crear lista` y `Cerrar` siempre accesibles y cierra el panel antes de abrir canales/acciones de una lista.
- `Organizar mis listas` deja de usar `AlertDialog` con XML y pasa a un panel Compose fullscreen para evitar el cierre de Fire OS al adjuntar `ComposeView`.
- El aviso de nueva actualizacion disponible usa ya el panel Compose fullscreen, manteniendo las acciones de instalar y luego/cerrar.
- Los dialogos XML antiguos que contienen `ComposeView`, como `Organizar mis listas`, reciben ViewTree owners de forma recursiva antes de adjuntarse para evitar cierres por `ViewTreeLifecycleOwner not found`.
- Los dialogos con entrada de texto pasan a un panel Compose reutilizable con campos nativos: alias/tag de canal, nombre de listas, URL/token de catalogo offline y PIN parental.
- Los paneles informativos y de confirmacion de ajustes pasan a Compose, incluyendo resumenes, diagnosticos, estado de instalacion y confirmaciones de borrado/reset.
- El menu `TV y guia` pasa a llamarse `TV/EPG`, abre con la primera opcion enfocada y reordena `Guia timeline` como primera accion y `Este canal` como ultima.
- Los menus de herramientas, ajustes y acciones rapidas pasan a un panel Compose unificado, con filas mas legibles y comportamiento de atras/cerrar consistente.
- El VOD visual deja de montar `ScrollView` + `RecyclerView` manuales y pasa a un panel Compose con filtros y carruseles horizontales.
- La zona central de filas de la guia Timeline pasa a Compose; cabecera, escala y detalle se mantienen en los componentes Compose ya existentes.
- `La 1 UHD` usa ahora un servidor DASH local vivo para refrescar el MPD de Vodafone en cada recarga, evitando segmentos caducados y errores 404.
- El MPD local de `La 1 UHD` escapa el `$` de `LIVE$CUP...` al absolutizar segmentos para evitar templates DASH invalidos.
- El resolver prioriza el MPD local parcheado para directos DASH ClearKey, evitando que `La 1 UHD` vuelva a cargar el manifest original sin `default_KID`.
- Los directos DASH ClearKey pasan por resolucion previa de StreamInfo para que `La 1 UHD` use el manifest local parcheado antes de reproducir.
- `La 1 UHD` parchea localmente el MPD directo de Vodafone con `cenc:default_KID` para que ExoPlayer use la key ClearKey del catalogo offline.
- `La 1 UHD` ya no arranca limitada por el cap fijo de compatibilidad, permitiendo pista HEVC 2160p en Fire TV 4K compatibles.
- El overlay lateral muestra el contexto activo de plataforma o grupo en la cabecera, aprovechando el espacio derecho del bloque de informacion.
- Los DASH ClearKey de Vodafone servidos desde `vfsmartcdn.gb.vodafone.es`, como `La 1 UHD`, se tratan como reproduccion directa offline para evitar la ruta proxy que no arrancaba.
- En moviles, el arranque soporta la recreacion de Activity por orientacion sin cerrar el executor que aun pueden usar callbacks pendientes.
- La app acepta el nuevo baseline de catalogo firmado cuando la bajada solo afecta a live y el total del catalogo sigue dentro de margen.
- La barra tactil inferior incorpora `Plataforma`, un selector nativo para cambiar entre filtros/plataformas/grupos en moviles.
- Se desactiva temporalmente la cabecera tactil de filtros de la lista movil para evitar el cierre al iniciar de la beta 138.
- La lista movil muestra una cabecera `Plataforma / grupo` con botones grandes `◀` y `▶` para cambiar de plataforma/grupo desde la propia lista.
- En moviles, el acceso a la lista se llama `Canales` y el selector superior deja claro que cambia entre plataforma/grupo.
- El arranque en moviles ignora callbacks tardios de catalogo/reproduccion si la Activity ya esta cerrandose por el cambio de orientacion inicial.
- Pluto HLS sin DRM se publica como directo en el snapshot offline y evita caer a modo compatibilidad/proxy.
- Runtime live/VOD deja de contarse como trafico servidor cuando el backend solo entrega el manifest y el peso del stream va a CDN externa.
- `Actualizar catalogo local` vuelve a estar en el primer nivel de `Herramientas` y en el hub inicial.
- El VOD de Runtime del snapshot offline apunta al host publico principal para no depender de rutas `/api/vod` inexistentes en `fire.tvbep.com`.
- Las rutas directas externas detectadas por el player ya no se degradan a proxy por venir precedidas de una URL interna `/live/{id}`.
- Las CDN externas con rutas que contienen `/live/` ya se muestran como trafico directo.
- Pluto usa HLS directo como ruta inicial para acelerar el zapping y conserva compatibilidad como fallback.

### Menus simplificados

- `Herramientas` pasa a un menu principal mas simple para usuarios no tecnicos.
- El boton `Herramientas` vuelve a abrir directamente ese menu en lugar de mostrar solo el texto introductorio.
- Al pulsar `Atras` dentro de un submenu, la app vuelve al nivel anterior en vez de cerrar todo el menu.
- La parte tecnica queda recogida en `Ajustes avanzados`.
- Guia, grabaciones, busqueda y control parental quedan mas visibles en el flujo diario.
- El copywriting se ha rehecho para usar nombres mas naturales desde el mando, como `TV y guia`, `Peliculas y series`, `Familia y control` y `Ver mis grabaciones`.

### Migracion a Compose

- Los overlays principales, el HUD inferior y varias listas modales del modo offline ya se apoyan en componentes Compose.
- Busqueda global, mini guia, resultados EPG, listas personales y canales rapidos comparten ahora una base visual mas consistente y adaptable a movil/tablet.
- La guia Timeline ya mueve a Compose la cabecera, la escala horaria, la placa de canal, la ficha de detalle y el contenido visual de los bloques.
- La rejilla central conserva la navegacion clasica con mando, pero internamente ya queda separada en filas, strips y bloques visibles para iterar mas rapido sobre errores reales.
- El HUD inferior reparte mejor los accesos rapidos y el overlay de canales da mucho mas espacio a la lista en TV, escondiendo paneles poco utiles arriba.
- El HUD de TV vuelve a una fila unica mas ancha y la seleccion izquierda/derecha deja de envolver de forma confusa.
- El HUD de TV recupera todas las acciones visibles en una sola fila real, con botones repartidos de forma uniforme y refresco inmediato del foco visual al navegar con mando.
- La barra tactil inferior deja de cerrar la app en moviles Android al evitar el doble scroll horizontal entre XML y Compose.
- El HUD principal ajusta mejor el ancho util y la tipografia para que los botones no queden abreviados con puntos suspensivos.
- Los dialogos fullscreen de `Guia timeline` y `EPG visual` ya heredan correctamente el ciclo de vida del activity, evitando cierres al abrirlos en Fire TV.
- La app ya aparece como lanzador Android normal en telefono/tablet y el hub tactil reorganiza sus accesos en varias filas cuando el ancho real es reducido.
- El overlay lateral mantiene la cabecera con contexto de canal/plataforma mientras sigue priorizando la lista.
- La activacion offline ya etiqueta cada dispositivo con su nombre real y su tipo, en vez de registrar todos como `Fire Stick offline`.
- La app ya no declara orientacion fija en el `manifest`, evitando que algunos launchers de movil oculten el icono por tratarla como app solo landscape.
- Los heartbeats distinguen mejor reproduccion directa frente a trafico que realmente pasa por backend, evitando falsos positivos en Actividad offline.
- El heartbeat expone tanto la reproduccion directa real como el valor original del catalogo para que el dashboard no confunda rutas directas con perfiles heredados del snapshot.
- La reproduccion directa real solo se marca ya cuando la ruta final del player es directa de verdad, no solo porque el snapshot lo sugiera.
- En standalone, `auto` intenta ahora directo primero tambien en muchos canales DRM antes de caer a proxy, con el objetivo explicito de minimizar trafico saliente desde el servidor.
- En standalone, HLS y DASH normales dejan tambien de pasar por `proxy_manifest` por defecto cuando existe una ruta directa usable.
- Los modos `proxy` aprendidos automaticamente dejan de imponerse en siguientes arranques standalone para que la beta pueda reevaluar rutas mas directas.
- Los canales Adultos/Hot se mantienen en una ruta mas conservadora mientras afinamos su compatibilidad concreta.
- El heartbeat deja de marcar como `trafico servidor` rutas que ya son directas de verdad solo porque el perfil historico del canal siga siendo `proxy_manifest`.
- Las rutas DRM directas resueltas en `auto` vuelven a enseñarse como `Directo DRM` en lugar de quedar confundidas como `Proxy DRM`.
- Movistar normal deja de intentar primero una ruta DASH ClearKey directa que ahora mismo esta devolviendo `Source error` en Fire TV, recuperando asi un arranque mas estable por la ruta compatible.

### Control parental

- El dashboard offline ya puede marcar bouquets, filtros y canales concretos como protegidos.
- La app guarda un PIN local por dispositivo para desbloquear temporalmente ese contenido.
- El VOD adulto tambien puede quedar protegido desde el snapshot con `parental_vod_adult`.
- `Control parental` muestra ya un menu operativo con estado, desbloqueo, cambio de PIN y borrado, en lugar de parecer solo un resumen.

### Calidad real de reproduccion

- La app mide la calidad real del stream reproducido usando Media3: resolucion, codec de video, fps, bitrate y codec de audio.
- Los heartbeats de reproduccion envian la calidad real al servidor para verla en Actividad offline.
- El dialogo de diagnostico de reproduccion muestra la calidad detectada del canal actual.
- El overlay inferior completo muestra la calidad real junto al nombre, ruta y EPG.
- El aviso superior deja de repetir el nombre del canal cuando ya aparece en el panel inferior.
- El cartel inferior de zapping muestra la calidad real del canal y se refresca cuando Media3 detecta el formato.
- El cartel inferior de zapping permanece visible unos segundos mas para dar tiempo a ver la calidad detectada.

### Estabilidad EPG

- La guia cruza programas remotos y del snapshot por `channel_id`, `tvg_id` y nombre, para cubrir plataformas donde el `channel_id` no coincide con el catalogo filtrado offline.
- El exportador manual de snapshots rellena la guia con fallback desde `/api/epg/now` cuando el endpoint por canal no devuelve datos.
- La app evita quedarse en bases que no publican `/api/epg`, como `fire.tvbep.com`, y prueba automaticamente el host publico principal y la base LAN.
- La app offline vuelve a mostrar EPG cuando el snapshot local no trae `epg`, consultando el backend offline con el token de activacion.
- Las llamadas EPG en modo independiente ya envian `Authorization`, `X-DRBEP-Access-Token` y `X-DRBEP-Device-Id`.
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
- Confirmar que un bouquet protegido no aparece en `Todos` mientras la sesion siga bloqueada.
- Confirmar que al abrir un bouquet o canal protegido se pide el PIN y el desbloqueo dura unos minutos.
- Confirmar que el nuevo menu principal deja a mano `Guia`, `Grabaciones`, `VOD`, `Buscar y recientes` y `Control parental`.
- Confirmar que `Ajustes avanzados` sigue dando acceso a playback, multiview, diagnostico e instalacion.
- Confirmar que la actualizacion beta instala correctamente y reporta version `100`.
