# Plan consolidado Android — 30/10/2026
Referencia vigente de prioridades. Complementa pendientes-20261029.md, que
conserva los resultados completos, pruebas anteriores y dependencias.
Base revisada: TeVas v0.5.0 e598b3f y v0.5.1 46f0b26; analisis de codigo,
sin afirmar rendimiento real ni copiar su implementacion GPL.
Desarrollo exclusivamente .185. No desplegar .223, publicar ni instalar
sin el alcance autorizado. TV no confirmada: todas las pruebas Fire pendientes.

## Orden de trabajo
1. Foco ligero y primera entrada TV/miniatura (EN CURSO).
   Separar decoracion de foco de composicion del icono/texto. Auditar lateral:
   ya tiene ancho fijo68dp y etiquetas Popup, no animar su ancho ni rehacerlo.
   Primer cambio: MainRailItem, dibujo de fondo/borde y escala por capa;
   etiqueta aislada. Conservar semantica, acciones, seleccionado y foco inicial.
   Continuar pico CPU de montaje ~180ms, dos modelos/18snapshots iniciales.
   No volver a optimizar logos sin medicion. Comparacion ABBA release,
   mismo canal/grupo, frio/caliente, CPU/frame/memoria y video.
2. Anchos adaptativos de categorias de peliculas/series.
   Medir pocos nombres, limites segun pantalla, dos lineas cuando proceda;
   ancho estable durante navegacion, ajuste manual prevalece.
   Mantener densidad, filtros, foco y uso tactil. No ampliar lateral principal.
3. Cache de imagenes y mantenimiento diferido.
   Inventariar caches y limites existentes; presupuesto global segun espacio.
   Retirada gradual de entradas antiguas, sin vaciado masivo ni IO al arrancar.
   Conservar imagen visible/seguir viendo. Mostrar uso en panel web.
   Medir lecturas/red/asignaciones y espacio, no confundir disco y RAM.
4. Programacion de actualizacion de listas.
   Hora inicial, intervalo y varias veces al dia. Reutilizar coordinador;
   no duplicar tareas, reanudar tras inactividad, probar zona/DST y cancelacion.
   Respetar correcciones manuales, permisos y prioridad de reproduccion.
5. Biblioteca / Continuar viendo enriquecido.
   Fondo/logo/sinopsis del elemento seleccionado; cache acotada, peticiones
   deduplicadas y descarte de respuestas antiguas. Reutilizar TMDB existente.
   No recargar portada con VOD ni modificar tipografia restaurada.
6. Miniguia: cerrar comparacion equivalente canal/dia/seleccion;
   desplazamiento, ficha, ayer/hoy, foco y retorno sin perder video.
7. XMLTV extremo:1000canales/168000eventos con reproduccion; frio/caliente,
   cancelacion, memoria y retirada exclusiva del fixture. No reactivar
   COOPERATIVE_IMPORTS, cuyo ensayo fue descartado.
8. Caja negra: contrastar hitos por reproductor/generacion, coste con panel
   abierto/cerrado; ampliar resolucion previa a prepare y tiempos HTTP.
   No guardar tokens, URLs sensibles ni claves.
9. Parada e inactividad: validar individual/usuario en directo, miniatura,
   VOD, catchup, multiview/PiP, conservando progreso y otros usuarios;
   HDMI breve/apagado prolongado y aviso con mando/tactil.
10. Incidencias separadas: M+Indie HLS/catchup sigue abierto (ultimo502).
    Verificar identidad/origen sin sustituir canal; conservar La1/Vamos.
    EPG Deportes7/8 depende de fuente; seis altas ISM requieren identidad
    oficial. No modificar asociaciones existentes por similitud.
11. Xiaomi: OTA sin borrar datos, gestos, seek/pausa catchup, series,
    audio/subtitulos, agenda, busqueda y vuelta TV; requiere tablet disponible.
12. Cierre: consolidar candidata y evidencias, comparar Fire y tablet;
    commit/push/publicacion solo cuando se soliciten. No cambiar stable.

## Ya implementado: no repetir
Catchup indexado/paginado y filtros/Guardados; TMDB y trabajador/panel;
Plex incremental; agenda; Palantir; miniguia por dias; diagnostico de red;
EPG IB3/17canales y conservacion en sync ISM.
Mejoras locales30/10 pendientes de Fire: horarios, pertenencia a grupos,
favoritos, perfiles/nombres, orden limpio. 1049pruebas correctas al iniciar
este bloque. No sumarlas como si fueran ganancia global de entrada TV.

## Decisiones conservadas
Fuente anterior; no introducir logo de espera en cada zapeo; no portada VOD
pesada; no limpieza total de imagenes. OpenWebif/Trakt/AceStream/ISML siguen
opcionales o en su hilo propio, no se incorporan automaticamente.
Sin tele se permiten revision, codigo, tests y benchmarks .185; no cerrar
fluidez por compilacion, pruebas JVM, ADB Awake ni pantalla negra.

## Primer bloque preparado
30/10: foco del lateral separado en dibujo y etiqueta;1049 pruebas correctas,
release ARM32 beta compilado. Detalle: foco-lateral-20261030.txt.
Pendiente medicion real Fire, sin instalacion/publicacion. Proximo paso de
este bloque: auditar otros controles que recomponen por decoracion de foco,
sin cambiar textos/estado funcional ni asumir que todo debe pasar a Canvas.

## Continuacion del primer bloque
30/10: decoracion de foco de Buscar/Ajustes, accesos rapidos y miniatura
movida a fase de dibujo. Callbacks, requesters y reproductor conservados.
Detalle y pruebas pendientes: foco-controles-20261030.txt.
No hay nueva medicion Fire ni se cierra el pico de entrada a TV.

## Segundo bloque preparado
30/10: selector superpuesto de peliculas/series importadas con ancho adaptativo
y opciones de dos lineas, sin rail permanente nuevo ni cambios en filtros.
Detalle: categorias-adaptativas-20261030.txt. Fire/tablet pendientes.
Siguiente bloque: inventario y presupuesto de caches de imagenes, evitando
limpieza al arrancar y vaciados completos.

## Tercer bloque en curso
30/10: presupuesto de disco Glide32..128MiB segun espacio libre; memoria
conservada. Limpieza SVG diferida/coalescida, max32borrados por pasada,
sin escaneo antes de devolver la imagen. Sin vaciados automaticos.
Detalle: cache-imagenes-20261030.txt.
Pendiente completar informacion de uso/presupuesto en panel web y validar
IO/hit rate/imagenes en Fire. No cerrar con tests JVM.

Validacion del mantenimiento de cache:1060 pruebas correctas y compilacion
release final correcta. Pendiente panel web y validacion real de dispositivos.

## Panel de cache preparado
30/10: tarjeta Diagnostico/Cache de imagenes con consulta manual, uso Glide/SVG,
presupuesto realmente configurado y hora de medicion; reutilizacion30s.
Ruta autenticada de solo lectura, sin polling ni borrados.
Detalle: cache-panel-20261030.txt. Desarrollo del bloque3 preparado;
validacion Fire de cache, panel e impacto sobre video sigue pendiente.
Siguiente desarrollo: programacion de actualizaciones de listas (bloque4).

## Cuarto bloque iniciado
30/10: motor puro de horarios por dia preparado y probado, con medianoche,
DST, codificacion y agrupacion de franjas perdidas. NO conectado al worker:
automatico12h sigue igual. Detalle: horarios-listas-20261030.txt.
Pendiente persistencia/formulario/cola y pruebas de cancelacion/reinicio;
Fire sigue pendiente. No presentar esta base como funcion ya disponible.

## Cuarto bloque integrado en candidata
30/10: horarios por lista guardados transaccionalmente, formulario web y
trabajador opcional15min conectado a cola existente. Legacy12h/guias conservados.
Deduplicacion con bloqueo compartido y marca persistente de franja.
Detalle: horarios-listas-integracion-20261030.txt.
Pendiente Fire/WorkManager real y respaldo DB antes de adoptar esquema10.
Sin instalar/publicar. Siguiente desarrollo: biblioteca/Continuar viendo.

## Quinto bloque preparado parcialmente
30/10: fondo de la ficha seleccionada y cache LRU32 de sinopsis, consultas
compartidas y cancelacion de respuestas obsoletas. Carrusel/acciones conservados.
Detalle: biblioteca-seleccion-20261030.txt. Logos de titulos pendientes: no los
expone el resolvedor actual. Sin instalar/publicar; validacion Fire pendiente.

## Continuacion de biblioteca
30/10: consultas de artwork sin cache compartidas y cancelacion individual;
FIT_CENTER aplicado tras recibir la imagen (el cargador lo sobrescribia).
Detalle: biblioteca-imagenes-compartidas-20261030.txt. Fire pendiente,
sin instalar/publicar; logos de titulos siguen pendientes.

## Sexto bloque: comparacion preparada
30/10: registro miniguia por fases y acierto de cache; resumidor numerico
por filas/cache y pruebas de equivalencia. Sin cambio funcional ni nueva
optimizacion sin evidencia. Detalle: miniguia-fases-20261030.txt.
Comparacion ABBA y fluidez reales siguen pendientes del Fire.
Siguiente bloque abordable sin TV: preparar el ensayo XMLTV extremo.

## Septimo bloque: XMLTV grande
30/10: corpus existente1000canales/168000eventos reutilizado; cancelacion
por bloques para no esperar a una etiqueta tras comentarios largos.
Detalle: xmltv-cancelacion-20261030.txt. Carga SQLite, cache/cancelacion con
video y retirada segura siguen pendientes del Fire. Sin instalar/publicar.

## Octavo bloque: detalle HTTP preparado
30/10: cabeceras/lectura/total y resultado numericos por llamada HttpClient,
solo diagnostico ampliado, sin URLs ni cuerpos. No cambia transporte.
Detalle: caja-negra-http-20261030.txt. Correlacion previa a prepare y
validacion real zapeo/multiview/coste panel siguen pendientes. Sin instalar.

## Correlacion previa a prepare preparada
30/10: enlace explicito por operacion/generacion desde beginPlaybackAttempt
a prepare, con campo resolution separado de hitos existentes.
Detalle: caja-negra-resolucion-20261030.txt. No correlaciona peticiones HTTP
por proximidad ni mide trabajo anterior al controller. Fire pendiente.

## Noveno bloque: validacion de parada preparada
30/10: hitos numericos de progreso/parada/confirmacion, sin cambiar politicas
ni orden de ejecucion. Tres casos limite de inactividad añadidos.
Detalle: parada-fases-20261030.txt. Audio/video/trafico/progreso persistente
y otros usuarios siguen pendientes de comprobacion fisica. Sin instalar.

## Presentacion de diagnosticos completada en candidata
30/10: panel muestra resolucion previa, HTTP y fases de parada con valores
faltantes explicitos y sin afirmar parada fisica. Sin nuevo polling.
Detalle: panel-diagnostico-fases-20261030.txt. Validacion visual Fire pendiente.
Los puntos10/11 requieren contexto real/dispositivos; no se declaran resueltos.

## Correccion de navegacion reportada
30/10: retorno explicito al video activo desde catalogo, sin volver a cargarlo.
Detalle: retorno-video-catalogo-20261030.txt. Pendiente validacion Fire.

## Plan aparcado por peticion del usuario
30/10: pausar nuevos bloques Android hasta poder encender el Fire.
Conservar cambios y comprobaciones pendientes; sin instalar/publicar.
Prioridad temporal: estudio del dashboard, documentado en backend
docs/estudio-dashboard-20261030.md. No implica despliegue ni cierre del plan.

## Validacion retomada 30/10
Usuario confirma Fire encendido y autoriza recorrer pruebas, commit/push y nueva beta.
Estudio dashboard conservado en backend docs/estudio-dashboard-20261030.md y aplazado.
Respaldo APK/base antes de migracion9 a10; stable y backend sin modificaciones.
Evidencias privadas: /srv/drbep-dev/reviews/android-close-20261030.

## Resultado del pase con dispositivos
Candidata785 instalada en Fire y Xiaomi, respaldos y migracion verificados.
XMLTV168000, cache y horario real completados; fixtures retirados sin alterar
los datos originales. Consultar validacion-beta785-20261030.md para medidas
y pendientes: no se declara cerrado el pico inicial ni la parada fisica.
