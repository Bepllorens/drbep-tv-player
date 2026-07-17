# DRBEP Offline App - posibles mejoras

Fecha: 2026-07-17

Base analizada: app offline en torno a `2.0.319-beta-fast-startup-timeline`, con la migracion a Compose muy avanzada, arranque rapido con cache local, EPG progresiva, VOD operativo y reproduccion directa/proxy segun plataforma.

## Resumen ejecutivo

La app ya esta en una fase funcionalmente solida: arranca con catalogo local cuando puede, el HUD y los paneles principales estan en Compose, la guia timeline funciona, hay soporte Fire TV/tablet/movil, y las plataformas principales reproducen con rutas diferenciadas.

El siguiente salto deberia centrarse en cuatro ideas:

- Reducir aun mas el tiempo de arranque real y percibido, sobre todo cuando cambia el catalogo.
- Hacer mas visible lo que esta pasando: carga de catalogo, EPG, VOD, rutas de reproduccion y errores.
- Pulir la experiencia adaptativa en Fire TV, tablet y telefono sin duplicar logicas.
- Limpiar deuda tecnica ahora que Compose ya cubre casi toda la UI.

## Bloque 1: arranque, catalogo y cache

Objetivo: que la app nunca parezca bloqueada ni arranque en negro, incluso cuando el catalogo cambia.

Mejoras propuestas:

- Separar el snapshot inicial en secciones: directo/live, VOD, EPG, metadatos e imagenes.
- Cargar primero solo lo imprescindible para reproducir TV en vivo.
- Hidratar VOD, caratulas y secciones pesadas en segundo plano.
- Mantener una huella por seccion, no solo una huella global de catalogo.
- Reutilizar el ultimo catalogo live bueno aunque falle una seccion secundaria.
- Medir y mostrar en diagnostico tiempos de parseo: descarga, validacion, live, VOD, EPG e imagenes.

Beneficio esperado: menos pantalla negra tras cambios de catalogo, menor memoria en Fire Stick y una recuperacion mas elegante si una parte del snapshot llega mal.

Riesgo: algunas secciones como VOD podrian aparecer unos segundos despues del directo. Hay que acompanarlo con estados visuales claros.

## Bloque 2: EPG y guia

Objetivo: que la EPG sea fiable, progresiva y facil de diagnosticar.

Mejoras propuestas:

- Mostrar progreso visible cuando la guia este completando plataformas: `Cargando guia de Movistar ISM`, `Tivify lista`, etc.
- Cachear la ventana timeline por plataforma y rango horario.
- Priorizar la EPG de la plataforma activa y despues cargar el resto en background.
- Exponer en diagnostico por plataforma: canales con EPG, eventos cargados, ultimo lote y errores.
- Crear una pantalla de salud de EPG en el dashboard offline para ver asociaciones rotas, canales sin `tvg_id` y duplicados.
- Anadir fallback claro cuando solo hay `ahora/despues` y aun no esta el timeline completo.

Beneficio esperado: menos confusion cuando la guia tarda, y mas facil detectar si el fallo esta en la app, en el snapshot o en asociaciones del backend.

## Bloque 3: reproduccion, calidad y zapping

Objetivo: mantener zapping rapido sin sacrificar estabilidad ni calidad.

Mejoras propuestas:

- Definir perfiles de ruta por plataforma: directo DASH, directo Smooth, proxy DRM, proxy auto, smart proxy y fallback.
- Registrar por reproduccion: ruta elegida, calidad maxima detectada, calidad real seleccionada, tiempo a primer frame y si hubo fallback.
- Evitar fallbacks lentos si la ruta rapida ya funcionaba previamente para ese canal.
- Aplicar fallback solo tras error real o timeout corto, no por defecto.
- Crear una politica de calidad por plataforma, por ejemplo Orange siempre intentar perfil maximo antes de degradar.
- Revisar microcortes con metricas de buffer: `buffer underrun`, `dropped frames`, `manifest refresh`, `license latency`.
- Mostrar en diagnostico del canal una etiqueta entendible: `Directo`, `Directo con licencia`, `Proxy DRM`, `Fallback compatibilidad`.

Beneficio esperado: preservar el zapping rapido que ya funciona bien y tener pruebas objetivas cuando una plataforma cae a baja calidad.

## Bloque 4: tablet y telefono

Objetivo: que la app no sea solo "Fire TV adaptado a tactil", sino una experiencia comoda en pantalla tactil.

Mejoras propuestas:

- HUD inferior adaptativo: botones centrados, scroll horizontal si no caben, y tamanos distintos para telefono/tablet/TV.
- Selector de plataforma tactil mas evidente, tipo bottom sheet con logos y buscador.
- Ocultar controles puramente de mando en telefono, por ejemplo giro libre si no aplica.
- Gestos simples: tap para HUD, swipe lateral para canales, swipe vertical para volumen/brillo si interesa.
- VOD con rejilla virtualizada y paginacion real para listas enormes como Movistar.
- Modo retrato basico para telefono: mini reproductor arriba y lista/controles abajo.

Beneficio esperado: menos friccion al usar la app en movil o tablet y menos casos especiales parcheados en el HUD.

## Bloque 5: VOD, U7D y grabaciones

Objetivo: que las secciones no lineales tengan el mismo nivel de pulido que la TV en vivo.

Mejoras propuestas:

- Unificar estados de carga: solicitando manifest, obteniendo licencia, preparando player, cargando ficha.
- Para VOD pesado, usar busqueda y paginacion server-side cuando sea posible.
- Mejorar U7D con caratulas: usar imagen del evento, imagen del programa, logo de canal o placeholder por prioridad.
- Anadir barra de progreso y controles consistentes para VOD/U7D/timeshift.
- En grabaciones, mejorar filtros: completadas, programadas, fallidas, por canal y por fecha.
- Mostrar causa de fallo de grabacion con copy claro y accion sugerida.

Beneficio esperado: menos pantallas negras y una experiencia mas profesional cuando una peticion tarda.

## Bloque 6: dashboard y observabilidad

Objetivo: que desde el dashboard se entienda rapidamente que esta pasando en cada dispositivo.

Mejoras propuestas:

- Tarjeta de salud por dispositivo: arranque, version, canal update, catalogo, EPG, ultima reproduccion y ultimo error.
- Mostrar nombre real del dispositivo, modelo, tipo, usuario, version y canal de update agrupado por usuario.
- Mostrar ruta de trafico real: video directo, licencia por servidor, proxy DRM o servidor completo.
- Historico corto de reproducciones con canal, plataforma, ruta, calidad, error y tiempo a primer frame.
- Boton de diagnostico que capture automaticamente logcat, estado de player y snapshot actual.
- Avisar si un dispositivo reporta `EPG 0 / 0`, catalogo reducido, fingerprint desfasado o version obsoleta.

Beneficio esperado: menos investigacion manual con `adb logcat` y menos dudas sobre si el trafico es directo o de servidor.

## Bloque 7: seguridad

Objetivo: reducir al minimo el valor de un APK o catalogo copiado.

Mejoras propuestas:

- Mantener fuera del catalogo todas las keys sensibles y pedirlas bajo demanda.
- Usar concesiones cortas para licencias o tokens de reproduccion.
- Asociar permisos a usuario y dispositivo para que quitar permisos en dashboard corte reproduccion aunque conserve catalogo.
- Cifrar caches locales sensibles con claves del dispositivo cuando aplique.
- Firmar secciones del snapshot y rechazar partes manipuladas.
- Registrar auditoria de peticiones de licencia sin guardar secretos.
- Revisar que logs de app/backend nunca impriman keys, tokens completos ni URLs firmadas reutilizables.
- Redactar URLs efectivas en heartbeats y paneles de diagnostico: conservar host/ruta, eliminar query/fragmentos y tokens.

Beneficio esperado: alguien con APK y catalogo no deberia poder extraer material suficiente para compartir streams protegidos.

## Bloque 8: deuda tecnica y Compose

Objetivo: aprovechar que la migracion a Compose esta madura para simplificar codigo y reducir regresiones.

Mejoras propuestas:

- Extraer de `MainActivity` coordinadores claros: playback, catalogo, EPG, overlays, menus y input.
- Crear una maquina de estados para overlays: live HUD, timeshift, menu lateral, timeline, dialogos y tactil.
- Revisar XML legacy: mantener solo contenedor raiz, recursos y drawables necesarios.
- Consolidar componentes Compose reutilizables: botones HUD, tarjetas de canal, estados vacios, loaders y dialogs.
- Crear previews Compose para HUD Fire, HUD tablet, lista lateral, timeline y VOD.
- Anadir tests de navegacion/foco para Fire TV con escenarios criticos.

Beneficio esperado: menos bugs de foco, menos efectos colaterales al tocar HUD/menu y mas velocidad para evolucionar la UI.

## Bloque 9: testing y releases

Objetivo: que cada beta llegue con una comprobacion repetible antes de pasar a stable.

Mejoras propuestas:

- Smoke test ADB automatizado en `.16`: abrir app, esperar catalogo, zapear, abrir Guia, Info, Grab, HUD, VOD y U7D.
- Matriz minima de prueba: Fire Stick 4K/4K Max, tablet Android y telefono Android.
- Benchmark de arranque: primer arranque tras cambio de catalogo y segundo arranque con cache.
- Checklist beta a stable: versionCode superior, firma OK, APK descargable, actualizador interno OK y dashboard sin errores nuevos.
- Release notes generadas desde commits y documento de estado.

Beneficio esperado: menos regresiones repetidas y mas confianza al promocionar betas.

## Quick wins recomendados

- Mostrar en la app un estado visible cuando el catalogo esta validando o la EPG esta completando plataformas.
- Anadir al diagnostico local los tiempos de arranque y parseo por seccion.
- Ajustar el HUD tactil para que nunca corte botones en telefono ni deje huecos raros en tablet.
- Mostrar en dashboard la ultima ruta de reproduccion y calidad real por canal.
- Revisar logs para ocultar tokens, keys y URLs sensibles.

## Apuestas de mayor impacto

- Snapshot por secciones con live-first y VOD diferido.
- Politica server-driven de rutas/calidad por plataforma.
- Observabilidad completa de reproduccion: ruta, calidad, primer frame y fallback.
- Refactor de estados de overlay/foco para Fire TV y tactil.

## Orden de trabajo sugerido

1. Instrumentar tiempos de arranque, parseo, EPG y reproduccion.
2. Separar catalogo live-first y cargar VOD/imagenes en background.
3. Pulir HUD adaptativo de tablet/telefono y foco Fire TV con una maquina de estados.
4. Mejorar observabilidad en dashboard para dispositivos, rutas y errores.
5. Optimizar VOD/U7D con paginacion, estados de carga y caratulas.
6. Limpiar deuda tecnica de Compose y crear tests de navegacion/foco.

## Estado de implantacion

- 2026-07-17: primera pasada de los bloques 1 y 2 en app offline.
- El arranque standalone intenta reutilizar la cache completa parseada si la huella no ha cambiado.
- Si no hay cache completa valida, la app puede arrancar con un catalogo live-only para dejar TV disponible antes.
- El catalogo completo se hidrata despues en segundo plano, se guarda como cache de arranque y se aplica sin relanzar la reproduccion actual.
- Se anaden metricas en log por fase: normalizacion, permisos, parseo live, parseo VOD, filtros, total de parseo y duracion completa.
- La EPG progresiva reporta estado visible por lote/filtro: cargando, parcial, lista, timeout, error y completa.
- El diagnostico remoto incluye estado EPG, filtro activo, canales procesados, total estimado, ultimo lote, errores y tiempos.
- El router de mando separa comprobaciones `can...` de acciones reales: preguntar si se puede buscar ya no mueve la reproduccion.
- En HUD/timeshift con mando, izquierda/derecha ejecutan seek real cuando la barra de timeshift tiene foco, y los botones multimedia REW/FWD tambien hacen salto real.
- VOD/U7D comparten estado de carga observable: tipo de contenido, paso activo, detalle y tiempo de espera se muestran en UI y se envian en diagnostico/heartbeat.
- El heartbeat de reproduccion tambien reporta titulo y detalle de carga VOD/U7D para diagnosticar esperas largas desde dashboard sin logcat.
- El estado de overlays ya separa visibilidad y foco: repintar timeshift no roba foco al HUD inferior salvo que el usuario suba explicitamente a la barra.
- El HUD inferior desplaza automaticamente el carril horizontal hacia el boton enfocado cuando no caben todos los botones.
- El selector tactil de plataforma/grupo se abre centrado cerca del filtro activo para evitar buscar manualmente en listas largas.
- El estado periodico del dispositivo incluye resumen compacto de dashboard: filtro activo, EPG, VOD/U7D cargando, ruta, trafico, calidad y tiempos basicos de reproduccion.
- El estado periodico tambien envia `device_health_level` y `device_health_summary` para que el dashboard pueda pintar OK/loading/warning/error sin recalcularlo.

## Criterios de exito

- Segundo arranque con cache: catalogo util en menos de 2 segundos en Fire Stick moderno.
- Primer arranque tras catalogo nuevo: pantalla informativa visible y TV reproducible lo antes posible.
- Guia timeline abre una sola vez, sin parpadeo, y completa progresivamente.
- HUD Fire TV navegable con mando sin interferir con zapping cuando esta abierto.
- HUD tablet/telefono sin botones cortados ni solapamiento con timeshift.
- Dashboard distingue correctamente video directo, licencia por servidor y trafico proxy.
- Ningun secreto sensible aparece en catalogo, logs o diagnosticos visibles.
