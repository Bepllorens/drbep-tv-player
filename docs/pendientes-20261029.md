# Estado consolidado del plan — 29/10/2026, beta 784

Prioridades actualizadas en [plan-mejoras-20261030.md](plan-mejoras-20261030.md).
Este documento conserva el detalle historico y las evidencias. Los documentos anteriores conservan
evidencias historicas; sus frases "sin publicar", "ADB no autorizado" o versiones
antiguas no describen necesariamente el estado actual.
Consolidado con registros del .185 y las continuaciones de pruebas Fire del29/10.
Las continuaciones Android no modifican produccion ni publican otra beta.

## Entrega actual

- Beta 784 universal ARM32/ARM64 publicada y descargada con SHA256 verificado.
- Version: 2.0.784-beta-guide-refresh. Firma de distribucion verificada.
- 1025 pruebas unitarias correctas. Fuente/version publicada: Android ba21388.
- Mejoras de carrusel/miniguia/filas: commit 129ebb3.
- Fire: candidata privada ARM32/784 actualizada el29/10, con firma de pruebas;
  es distinta de la APK publica. Detalle y limites en el punto1. Tablet pendiente.
- Stable 582 y rescue 543 se conservaron en la publicacion.
- Backend: seguridad, EPG e informes en Git hasta 983bebb.

## Cerrado: no volver a implementar

- Catchup indexado/paginado, consultas de filtros, generos y Guardados optimizadas.
  Se conservan las equivalencias y benchmarks documentados; no prueban por si
  solos fluidez perfecta en dispositivo.
- TMDB: consultas de progreso/pendientes, filtros mixtos, indice para series con
  grupo y trabajador, pausa/reanudacion y gestion desde panel. Integrados en las
  entregas previas; no convertir de nuevo estos bloques en desarrollo pendiente.
- Buscador por origen, progreso agrupado, controles compactos y lateral configurable.
- Plex incremental desplegado y probado con sincronizaciones reales:
  97,471s -> 0,463s, 25 bibliotecas reutilizadas, sin perdida registrada.
- Agenda y favoritos deportivos disponibles y probados en Fire.
- Diagnostico de red bajo demanda y extension de caja negra implementados/publicados.
- Palantir: catalogo/cuentas, enlaces de origen, fechas de estreno, rediseño web,
  comprobacion de actualizacion y persistencia de ambas fechas corregidos.
- Miniguia por dias, seleccion recordada, fichas, paneles y distintivos HDR/Dolby.
- Beta 784: reemplazo atomico del dia para retirar horarios antiguos del carrusel;
  Guardados preservados, respuesta vacia/invalida no destructiva.
- Beta 784: cache de ventana de miniguia, claves estables y scroll al salir
  del area visible; menor trabajo grafico por fila TV.
- EPG: recuperado IB3 y 17 de los otros 19 canales; equivalencias revisadas por
  identidad oficial, variantes internacionales separadas de las regionales.
- Sync ISM real del 29/10: 158 canales existentes conservan IDs/diales/EPG;
  asociaciones iguales y 23786 eventos exactamente iguales. Seis altas nuevas.
- Seguridad Node/Brotli activada y documentada; limpieza/archivo privado verificado
  del .223 completados. Ultima comprobacion: 72% de uso, unos 41GiB libres.

## Pendiente real: orden recomendado

### 1. Primera entrada a TV y presion de memoria — mejora parcial

Objetivo: reducir el primer pico de Compose/medida/colocacion sin perjudicar video.
Referencia comparable Movistar HLS del 28/10:
- Primer acceso p95: 600ms antes y despues; sigue abierto.
- Recorrido filas p95: 93 -> 77ms en dos repeticiones.
- Fotogramas con retraso: 87,88% -> 81,86/81,91%; todavia elevados.
No atribuir todo el pico a los logos: su coste medido era menor que layout.
Medir mismo grupo/canal, frio/caliente, CPU/espera/frame y memoria.
Comprobar miniatura, mismo canal de fondo, pantalla completa y retorno.
Continuacion29/10: patrones de titulos reutilizados, mismo resultado;
benchmarkJVM caliente -55,74% tiempo/-77,50% asignacion, sin mejora CPU fria.
Prueba Fire con La1/HLS completada: no hay mejora global acreditada.
Ensayo de progreso/dibujo retirado; Fire restaurado a control784 compatible.
Presion de memoria/swap medida y tambien peor repeticion del control;
no atribuir automaticamente los picos a DRBEP ni a una fuga.
Traces idle sobrescribieron el anillo: no prueban eliminacion de mediciones.
Pantalla completa/retorno y miniatura en movimiento correctos al finalizar.
Detalle: tv-entry-20261029.txt. Pico de entrada y tirones siguen abiertos.
Nueva continuacion29/10: construccion de lista sin resolver programacion de filas
ocultas; snapshots completos al componer fila o buscar. Prueba Fire correcta.
Paso intermedio tvPage164-191ms; final25-37ms mas12-20ms de snapshots visibles.
Frame max medido en las dos trazas finales695/556ms frente804/725ms del control;
no atribuir una ganancia estadistica con solo dos muestras y memoria variable.
1029unitarios correctos; ficha, busqueda fuera de pantalla, pantalla completa/
retorno y video en miniatura comprobados. Candidata privada instalada, sin publicar.
Repeticion sin traza: entrada p95950ms, navegacion p95150ms (87,75%janky).
Paginado funciona, pero NO hay reduccion sostenida demostrada del pico global.
Conservar esta muestra desfavorable. Pendiente comparacion intercalada y sesion
larga, registrando memoria en cada pasada.
Detalle: tv-entry-visible-20261029.txt.
Reinicio Fire autorizado29/10 y comparacion intercalada: control396-508ms,
candidata390-435ms (maxframe en trazas cortas). Sin traza: entrada p95350ms,
navegacion p9542ms. Memoria disponible recuperada; no atribuir todo al codigo.
Miniatura estable35,5s:25fps,0nuevos descartes,0huecos>80ms.
No cierra estabilidad prolongada; layout aun200-230ms y janky sigue alto.
Estado/evidencia mas reciente: tv-entry-clean-fire-20261029.txt.
Continuacion29/10: indice visual ligero con Paint cacheado; mismos tamaño,
colores, recorte, semantica, foco y acciones. Ensayo TextMeasurer retirado.
Comparacion ABBA en una APK: entrada control500/350ms, directo350/350ms;
p95recorrido control77/69ms, directo73/65ms. Janky sigue alto; ganancia modesta,
sin evidencia de una reduccion sostenida del arranque con solo estas muestras.
Traza CPU: control layout254ms/210CPU, directo169ms/153CPU (una pareja).
No atribuir toda la diferencia a este cambio: tambien varian las esperas.
Miniatura final156s,24,99fps,0descartes y0huecos>80ms. 1029unitarios correctos.
Candidata privada784 instalada; sin publicar, commit/push ni cambios de servidor.
Paso anterior de este punto: tv-layout-cpu-20261029.txt.
Nueva continuacion29/10: coordinador de refresco por pantalla TV, con suscripciones
solo montadas; sin un temporizador/observador por fila. Pausa por visibilidad y
ciclo de vida, cancelacion por identidad y misma cadencia15s. 1035unitarios.
Una APK/ABBA: entrada control450/350ms, compartido300/300ms; p95recorrido
control53/81ms, compartido53/65ms. Ganancia observada limitada, n=2porvariante.
La pareja CPU NO mejora layout:180->181ms; no cerrar el pico inicial.
Trazas finales:0lotes ocultos, vuelta9lecturas en un lote2,503ms.
Sesion final10min29s con navegacion y pantalla completa:14137fotogramas en las
dos ventanas idle,25fps,0descartes y0huecos>80ms; sin reinicios del proceso.
Foco/busqueda/retorno correctos, memoria sin crecimiento sostenido observado.
Candidata privada784 instalada; sin publicar, commit/push ni modificar servidor.
Referencia vigente: tv-shared-refresh-20261029.txt.
Intento posterior29/10: NO se ha corregido el pico~180ms. Fire dormido; se
desperto, pero el usuario esta fuera y no confirma TV/HDMI. Captura por coordenadas
invalida; auxiliares de trazado no completan una muestra util. No hay cifras nuevas
de mejora/regresion. Herramientas compiladas en185, aun sin validacion de ejecucion.
Instrumentacion temporal retirada, APK anteriorc068107... restaurada/verificada,
auxiliar eliminado y salida de build limpia equivalente al paso previo.
Detalle y reanudacion: tv-mount-cost-pending-20261029.txt.
Validacion actual de TV/miniatura pendiente; mantener datos y correccion anterior.

Continuacion30/10 sin TV confirmada: Fire responde ADB y tras KEYCODE_WAKEUP
declara Awake/ON; no demuestra televisor/HDMI ni video. Sin instalar APK.
Horarios de filas TV: EpgScheduleFormatter reutiliza los dos tiempos ya
interpretados (antes cinco consultas para un intervalo valido). Sin cache de
canales/modelos, ni cambios de invalidacion, permisos, EPG o reproductor.
1037unitarios correctos, incluidas equivalencias de formatos/zona/DST.
Microbenchmark JVM caliente,133intervalos,4pasadas por variante intercaladas:
mediana54,660->42,703ms por100000intervalos (-21,9%),
asignacion77,6->58,4MB (-24,7%). No es una medida Fire ni ahorro de memoria
residente; el pico~180ms sigue abierto. Detalle: tv-schedule-20261030.txt.

Continuacion30/10, pertenencia a grupos: matches y visibleInHome comprueban
grupo principal/custom directamente, sin crear LinkedHashSet/ArrayList temporal.
Sin cache nueva ni reglas de permiso distintas. 1040unitarios correctos.
BenchmarkJVM con4000canales sinteticos,400000pares de comprobaciones/pasada,
ABBA repetido:mediana64,436->23,012ms (-64,3%);296->76,8MB asignados (-74,1%).
Ganancia local, no del arranque total ni RAM residente. Sin Fire/instalacion.
Detalle: tv-membership-20261030.txt. Mantener abierto el pico de montaje.

Continuacion30/10, favoritos: indice local solo de IDs favoritos, sin indexar
catalogo completo; filtro de proteccion solo para candidatos favoritos.
Sin cache persistente ni cambios de orden/proteccion.1044unitarios correctos.
Benchmark JVM4000canales/20favoritos,1000reconstrucciones:mediana92,559->27,412ms
(-70,4%),227,112->2,600MB asignados (-98,9%). No mide pantalla completa.
Pruebas Fire pendientes; detalle tv-favorites-20261030.txt.

Continuacion30/10, perfiles/nombres: perfil vacio inmutable compartido en
ChannelProfileStore; nombre consultado una vez por fila TV en vez de dos.
Sin cache de alias ni cambios de persistencia.1046unitarios correctos.
Benchmark JVM1Mfilas,4000IDs/10%alias:mediana18,743->9,574ms (-48,9%);
43,2MB temporales->residuo de medicion. Coste absoluto pequeno, no del layout.
Pendiente Fire; detalle tv-profiles-20261030.txt.

Continuacion 30/10: cache acotada del orden limpio de canales/categorias.
Mediana aislada JVM 188.017 -> 2.728 ms/1000 lecturas repetidas; asignaciones
-96.6%, mismo orden. Primera lectura sin ahorro; pendiente recorrido real Fire.
Detalle: tv-order-list-20261030.txt. No instalado ni publicado.

Siguiente: coste de subcomponer/aplicar filas y2modelos/18snapshots de la entrada;
validar diferencias reales de EPG/catalogo/permisos antes de reutilizar objetos.




Evidencia: pendientes-20261028.txt y reviews/carousel-tv-20261028.

### 2. Miniguia — completar comparacion equivalente

Cache y navegacion implementadas y probadas funcionalmente.
Falta medir antes/despues en el MISMO canal/dia/seleccion; las muestras
TV3/Vamos/M+Hits no permiten atribuir una ganancia porcentual.
Repetir desplazamiento, ficha, ayer/hoy y retorno sin perder foco ni video.

### 3. Xiaomi — actualizacion y validacion tactil

Probar OTA real a beta784 universal, sin desinstalar ni borrar datos.
Gestos/controles, avance/retroceso de catchup, pausa conservada, temporadas/
episodios, audio/subtitulos, agenda, busqueda y retorno a TV/miniatura.
Verificar caratulas y progreso tras reinicio. Requiere disponibilidad de tablet
y, para ADB, IP/puerto actuales. Publicar una APK no cierra esta prueba.

### 4. XMLTV extremo con reproduccion — validacion pendiente

Carga/cancelacion/cache/equivalencia ya tienen pruebas funcionales previas.
Falta cerrar fluidez y memoria bajo carga extrema mientras hay video/mando.
Corpus sintetico de 1000 canales y 168000 eventos, cache fria/caliente,
cancelacion y retirada exclusiva de datos del ensayo.
La mejora JVM (-15,59% lectura, -45,46% asignacion acumulada) no es una
medida de memoria residente ni de rendimiento Fire.
Evidencia: xmltv-load-20261026.txt y cierre-beta-776-20261026.txt.
No reactivar COOPERATIVE_IMPORTS: el ensayo no mejoro la fluidez y fue retirado.

### 5. Caja negra — validacion real y detalle de tiempos

Ya implementados manifiesto/datos/inicializacion de video y correlacion por
operacion/generacion, panel y exportacion acotada.
Falta contrastar hitos reales con zapeo, reintentos y multiview sin mezclar
reproductores; comprobar trazas sin hitos y coste con panel abierto/cerrado.
Como ampliacion de desarrollo quedan la resolucion anterior a prepare y el
desglose HTTP por peticion. No presentarlos como ya medidos.
Evidencia: startup-phases-20261027.txt.

### 6. Parada de sesiones e inactividad — pruebas fisicas

Backend/dashboard activos y cliente integrado. Falta comprobar de extremo a
extremo parada individual/de usuario en TV, miniatura, VOD, catchup,
multiview y PiP: sin audio/video/trafico, con progreso conservado y otros
usuarios intactos; caducidad, confirmacion y reintento.
Aviso de inactividad con mando/tactil, desconexion HDMI breve y TV apagada
prolongada. Las pruebas con reloj sintetico no cierran estos casos.
Evidencia: playback-control-despliegue-20261027.txt.

### 7. Catchup M+ Indie/HLS — incidencia separada abierta

Ultimo ensayo documentado: Cinema Paradiso, 60s sin fotograma, termina HTTP502.
No hay evidencia posterior de cierre. Reproducir con evento actual equivalente
si aquel caduco, verificar identidad exacta y origen; nunca otro canal como
sustitucion. Conservar rutas/asociaciones de La1 y Vamos.
No atribuir por defecto la causa a claves, derechos o formato.
Evidencia: plan-post-774-20261025.txt y backend catchup-hls-pendiente-20261025.txt.
Seek/pausa de Star tuvo prueba correcta previa; no marcarlo globalmente averiado.

### 8. EPG restante y catalogo nuevo — dependencias externas/revision

- M+ Deportes7/8: la fuente declara canales pero no aporta programas.
  No copiar eventos de otro canal. Revisar cuando exista EPG valida.
- Comprobar visualmente en cliente la EPG recuperada (backend ya verificado).
- Seis altas ISM: 14campeones a 19campeones, diales199-204. Revisar identidad
  oficial si se desean asociar; no asignar por parecido ni modificar las158previas.
La sincronizacion real no rompio las asociaciones: esa comprobacion esta cerrada.

## Ensayos y ampliaciones separadas

- HLS: lectura paralela audio/video activa, comparacion corta963,5->807,5ms.
  Sigue habiendo picos; no se promete igual mejora en todos los canales.
- Nueve flags experimentales siguen OFF. No reabrir como tareas obligatorias
  las variantes descartadas. Cada adopcion exige ensayo aislado comparable.
- Zapeo: pulsaciones separadas inmediatas y cancelacion probadas; la agrupacion
  con multiples repeats reales requiere mando fisico si se retoma. Un longpress
  ADB con un repeat no acredita una rafaga real de seis repeats.
- ISML: hilo anterior separado sin nueva implementacion validada segun el
  handoff CONTEXTO_ISML_PENDIENTE.md; no afirmar que esta resuelto.
- OpenWebif requiere receptor Enigma2/caso de uso. Trakt, AceStream/torrents,
  proveedor deportivo externo/avisos y configuracion DNS son opcionales,
  no bloqueos para cerrar estabilidad. No activarlos automaticamente.
- Grabacion local DRM descartada por el usuario; fuente anterior restaurada
  y tipografia Inter retirada: no siguen pendientes.

## Criterio de cierre y mantenimiento

Cada pendiente se cierra con version/dispositivo/pasos, resultado y evidencia.
Distinguir implementado, publicado, instalado y probado en dispositivo.
Una medicion entre canales distintos o con HDMI apagado no prueba una mejora.
Desarrollo en .185; pruebas visuales con TV en entrada Fire y mando disponible.
No desplegar/reiniciar .223 ni modificar stable al ejecutar pruebas del plan.
Respaldos historicos antiguos ahora en el archivo privado del185; ver backend
docs/archivo-produccion-20261029.txt para recuperar rutas antiguas de evidencias.

Documentos de entrega: beta784-release-20261029.txt, pendientes-20261028.txt.
Backend: docs/epg-remaining-20261029.txt, ism-sync-integrity-20261029.txt,
limpieza-produccion-20261029.txt y archivo-produccion-20261029.txt.
