# Validacion de beta 785 — 30 de octubre de 2026

## Artefacto y alcance
Candidata universal 2.0.785-beta-navigation-cache, ARM32 y ARM64.
SHA256 de distribucion: d043a15f0b1afc56dde88e4c471f1559d37456e7d62544a0a514e2870fb61723.
Xiaomi instalada con ese binario; Fire conserva su firma de pruebas anterior,
con payload equivalente. No se recompila despues de estas comprobaciones.
Backend y stable sin cambios. El estudio del dashboard queda aplazado en
el repositorio backend: docs/estudio-dashboard-20261030.md.

## Comprobaciones completadas
- 1118 pruebas JVM; 14 suites del panel web pasan en el codigo final.
- Respaldo previo de APK y base de datos de ambos dispositivos. Fire: migracion
  9 a 10 con todas las columnas y valores anteriores de 15 tablas iguales.
- XMLTV real en Fire: 1000 canales y 168000 eventos, todos los campos comprobados.
  La carga inicial tardo aproximadamente 478 s: no confundir con reutilizacion.
  Una segunda lista compartio la guia en 1,03 s sin nueva descarga HTTP.
  Cancelacion tras 2000 eventos, sin publicar una guia parcial.
- Horario diario: tres guardados, un trabajador unico y una sola importacion
  automatica completada; conserva y reutiliza la guia sin cambios. Forzar el
  job de Android antes de su ventana de WorkManager no ejecuta el trabajo;
  se espero a la ejecucion real. No se modificaron horarios del usuario.
- Eliminadas las tres fuentes sinteticas y sus trabajos/guias exactos.
  Comparacion posterior: same_data_as_backup=true y same_schema=true.
  Auxiliar del Fire retirado y servidor/reenvio de fixtures cerrado.
- Panel de cache: primera consulta 887,5 ms; segunda 8,6 ms reutilizando snapshot.
  Presupuesto de disco Fire 32 MiB; el uso existente puede superar temporalmente
  ese limite hasta aplicar la politica LRU. No se purgo el cache manualmente.
- Miniguia: abrir/cerrar, ayer/hoy, detalle y regreso. El registro Log.d no llegaba
  a release por R8; ahora hay eventos numericos bajo diagnostico ampliado.
  14 binds: 8 con cache, 6 sin cache. Preparacion mediana 970 us con cache y
  2875,5 us sin cache; no son tiempos de dibujo ni comparacion ABBA.
- Fire: apertura de filtros de peliculas/series, biblioteca, lista TV y retorno.
- Xiaomi: instalacion conservando datos, portada/TV, gesto horizontal de cambio
  de canal y regreso desde pantalla completa. No equivale a probar actualizacion
  OTA: se instalo el mismo binario por ADB. Su biblioteca de series esta vacia;
  no permite validar temporadas con datos reales.

## Rendimiento: limites observados
Referencia 784: entrada p95 200-300 ms; navegacion 44-53 ms.
785 recien instalada: entrada 550-650 ms; navegacion 65-73 ms.
Tras compilacion ART speed: 250/38 ms en un ensayo. El binario final dio
450/48 ms, mediana navegacion20 ms. No afirmar que el pico inicial esta resuelto
ni atribuir todo el cambio al codigo: la compilacion ART es un factor.
Video final: 63,6 s, 24,93 fps, mismo proceso y sin reinicio del decodificador;
3 frames descartados y 2 huecos mayores de80 ms, max217,42 ms.
Durante XMLTV: ~25 fps, sin nuevos descartes, un hueco150 ms.
Queda optimizacion de picos y una comparacion prolongada controlada.

## Pendiente explicito
- ABBA de construccion/dibujo de TV y miniguia y reproduccion prolongada.
- Parada fisica por HDMI/inactividad, audio/trafico y persistencia del progreso:
  tests de politica/diagnostico no prueban el televisor apagado.
- OTA completa en Xiaomi, gestos de avance en VOD/catchup y temporadas con
  biblioteca real; no presentarlos como comprobados por instalar una APK.
- Retorno al video desde catalogo: politica cubierta por tests; el recorrido
  reportado en Alfa no tiene validacion fisica en este pase.
- Logos de titulos: resolvedor actual no los proporciona.
- Dashboard: estudio guardado, rediseño no implementado.

Evidencias privadas (no incorporar bases, sesiones ni registros con contenido):
/srv/drbep-dev/reviews/android-close-20261030.
Publicacion y commits se verifican por separado al cerrar el turno.
