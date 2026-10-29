# Estado consolidado del plan — 29/10/2026, beta 784

Este documento es la referencia vigente. Los documentos anteriores conservan
evidencias historicas; sus frases "sin publicar", "ADB no autorizado" o versiones
antiguas no describen necesariamente el estado actual.
Revision documental basada en los registros de los repositorios del .185.
Este pase no repite pruebas del dispositivo ni consulta/modifica produccion.

## Entrega actual

- Beta 784 universal ARM32/ARM64 publicada y descargada con SHA256 verificado.
- Version: 2.0.784-beta-guide-refresh. Firma de distribucion verificada.
- 1025 pruebas unitarias correctas. Fuente/version publicada: Android ba21388.
- Mejoras de carrusel/miniguia/filas: commit 129ebb3.
- Pruebas Fire del codigo: candidata privada 783 del 28/10. La nueva APK 784
  no se ha instalado por ADB en este ultimo pase; tablet pendiente.
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
Siguiente bloque de desarrollo recomendado.
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
