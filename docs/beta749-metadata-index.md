# Beta 749: indexación TMDB desde el panel del dispositivo

Desarrollo en .185. No modifica backend, producción ni canales de actualizaciones.

## Uso
Panel web del dispositivo → Actividad → Completar metadatos TMDB.
Controles: Completar metadatos, Pausar, Continuar, Reintentar errores.
Al iniciar se incorporan a una cola SQLite las películas y una entrada por serie de
las generaciones activas de las listas. Las entradas ocultas no se incluyen; las
marcadas como adultas se omiten. Un cambio de generación o una lista eliminada no
se resuelve usando otra entrada: se omite la referencia antigua. Completar
metadatos de nuevo incorpora las nuevas entradas. No recorre episodios.

Los resultados se guardan por título. La caché válida se reutiliza, incluidas las
coincidencias compartidas entre listas; no se borran las fichas ni las selecciones
manuales. Los errores de transporte no se guardan como "sin coincidencia".
Reintentar errores vuelve a poner en cola únicamente los fallidos.

## Ejecución y límites
WorkManager mantiene un trabajo único. Cada tramo procesa hasta 20 entradas o
90 segundos y añade el siguiente tramo. No necesita que permanezca abierta una
pantalla ni el navegador. Android puede aplazar la ejecución en reposo; un cierre
forzado impide continuar hasta que el sistema permita arrancar la aplicación.
Necesita red, espacio disponible y sesión DRBEP. Si cambia la sesión, se detiene.
Pausa conserva el avance: una consulta ya iniciada puede terminar de guardar su
caché, pero no avanza la cola si el trabajo ha sido detenido o sustituido.

Hay una consulta de fondo a la vez y como máximo una nueva petición por segundo;
las lecturas de caché no esperan ese segundo. Las fichas enfocadas tienen prioridad
antes de iniciar el siguiente elemento. Las consultas de un mismo título se
coordinan; una consulta de fondo no bloquea las de otros títulos.

La cola y estados se guardan en SQLite (migración 7→8). WorkManager guarda solo un
identificador aleatorio del trabajo; no URLs, títulos, sesiones o credenciales.
Los controles HTTP conservan el PIN, token de panel, comprobación de origen y
límites de entrada del panel existente.

La pantalla Películas/Series ya no lanza tandas masivas al entrar. Resuelve la
ficha enfocada como antes y actualiza el catálogo visible cada 15 segundos solo
si el trabajador ha guardado nuevos resultados. No trabaja sobre pantallas ocultas.

## Archivos
- app/src/main/java/com/drbep/tvplayer/MetadataIndexWorker.kt
- app/src/main/java/com/drbep/tvplayer/ImportedMovieRepository.kt
- app/src/main/java/com/drbep/tvplayer/ImportedMovies.kt
- app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java
- app/src/main/java/com/drbep/tvplayer/PersonalPlaylistController.java
- app/src/main/java/com/drbep/tvplayer/DeviceWebPanel.java
- app/src/main/java/com/drbep/tvplayer/MainActivity.java
- app/src/main/assets/device-panel.html
- app/src/androidTest/java/com/drbep/tvplayer/MetadataIndexDeviceTest.kt
- app/src/test/java/com/drbep/tvplayer/DeviceWebPanelTest.java

No añade un servidor TMDB nuevo: reutiliza el endpoint de metadatos de DRBEP.
El avance se refiere a entradas revisadas, no a porcentaje de tiempo estimado.

## Validación del 16 de octubre de 2026
- 709 tests JVM, sin errores ni fallos.
- JavaScript del panel: node --check correcto; git diff --check correcto.
- Fire AFTKRT ARM32: MetadataIndexDeviceTest, 2 pruebas correctas, 0,74 s.
  Usan dos bases temporales eliminadas al terminar, títulos ficticios y caché
  sintética; ninguna petición a proveedores ni modificación de listas reales.
- Verificado: migración 7→8 sin perder catálogo, reapertura con progreso,
  cola sin duplicados, agrupación de series, caché sin red, corrección manual
  preservada frente a resultado automático atrasado y referencia de generación
  sustituida descartada.
- Pendiente: recorrido visual completo del panel e indexación masiva con vídeo
  para medir tiempos y fluidez. Las pruebas aisladas no acreditan ese rendimiento.

## Artefactos
Directorio: /srv/drbep-dev/artifacts/beta749-metadata
Beta universal ARM32/ARM64, versión 749. Sin publicar.
Release SHA-256: d3b8ef2f12fec1846b906b4c78f702729a72e14871fb084df506e78db327daa7
Fire SHA-256: 55a30325b433097551fc941b06601664aa7ede88bce47f06c6133471a8eaa8c9
La APK Fire usa el certificado de pruebas preexistente; no publicarla como release.
