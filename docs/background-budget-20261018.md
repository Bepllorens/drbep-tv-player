# Coordinación de tareas y consultas de catchup en beta 752

Trabajo realizado en el servidor de desarrollo .185. Sin publicación, despliegue del backend ni cambios de asociaciones, rutas de reproducción o claves. Conserva los cambios pendientes de beta 751.

## Cambios

BackgroundWorkBudget reserva intervalos breves al arranque y al comenzar una reproducción (8 segundos), y después de la entrada del usuario (750 ms). MetadataIndexWorker consulta ese estado antes de preparar la cola y entre títulos; no interrumpe peticiones en curso. La espera inicial está limitada a 15 segundos y después usa el reintento de WorkManager. La reproducción estable no bloquea indefinidamente la indexación. Esta primera integración cubre TMDB; no es todavía un planificador global de importación y descarga EPG.

La presentación del carrusel no consulta la página completa, su total ni sus géneros cuando el catálogo está cerrado. Conserva las comprobaciones de generación, filtros y visibilidad para no publicar una respuesta anterior sobre otra pantalla. Al cambiar de canal solicita los eventos ya almacenados independientemente de la precarga de guía.

U7dCatalogStore añade los índices programs_strip (scope,channel,start DESC,id) y programs_genres (scope,genre). La migración de esquema 2 a 3 es aditiva y evita volver a importar los datos legacy. No modifica eventos guardados ni contenido. Atención: instalar un cliente antiguo con esquema 2 después de esta migración requiere preparar una compatibilidad de esquema; no hacer downgrade ciego ni borrar datos.

## Validación

713 pruebas JVM, cero fallos y errores. Compilación universal ARM32 y ARM64. Firma de prueba compatible con la instalación existente del Fire, conservando datos; artefacto oficial separado para una futura publicación autorizada.

Prueba aislada en el SQLite del Fire con 80000 programas sintéticos y 60 repeticiones:
- Índice anterior: mediana 3436 microsegundos, percentil 95 de 8329.
- Índice nuevo: mediana 778 microsegundos, percentil 95 de 1000.
- Los 40 resultados y su orden coinciden exactamente. Desaparece la ordenación temporal.
- No utiliza bases de datos ni credenciales reales.

Caja negra de la primera prueba de dispositivo: para tiras de 40 entradas, 33 muestras de 751 con mediana de 78 ms y 38 de 752 con mediana de 8 ms. Hay diferencias de carga y estado de caché entre ejecuciones; esto es evidencia observacional, no una comparación controlada de toda la aplicación.

Recorrido de seis cambios de día:
- 751: 68 frames, 27 tardíos; p90 150 ms, p95 250 ms.
- Primera 752: 70 frames, 36 tardíos; p90 y p95 250 ms.
- La primera 752 aún actualizaba la guía. No se ha demostrado mejora global del dibujo; queda por aislar ese coste. No presentar la aceleración SQL como una aceleración equivalente de toda la pantalla.

La primera prueba detectó que el carrusel podía no solicitar de nuevo los eventos almacenados después de cambiar de canal. Se añadió una solicitud independiente por canal y generación. La compilación final muestra de nuevo la tira de DAZN LaLiga tras arrancar; verificación visual: dos pulsaciones Arriba pasan por timeshift al carrusel, e Izquierda selecciona DAZN Previa. La reproducción continúa y la vuelta a la miniatura se verificó en el recorrido.

## Repetición y evidencias

- tools/measure-catchup-navigation.py: desde catchup con foco en Hoy, seis cambios de día y recogida de gfxinfo. Los tiempos del host incluyen ADB y esperas; no son latencia de interfaz.
- tools/measure-catchup-sql.py: prueba sintética Linux.
- tools/CatchupSqlBenchmark.java: prueba SQLite ejecutable por app_process en el Fire, sin abrir bases de la aplicación.
- /srv/drbep-dev/artifacts/background-budget-752: APK, mediciones SQL y gfxinfo.
- Registros originales privados en el Mac: /private/tmp/blackbox752.zip. No incorporar el ZIP ni sesiones del panel a Git.


## Artefacto final instalado

Compilación final universal instalada conservando datos. Las 589 entradas del payload coinciden entre el APK oficial y el APK firmado para el Fire, excluyendo META-INF. Beta 752 no publicada; Git sin commit/push en esta tarea.

- release-752.apk SHA256: 883888e20c2d2c8784a3fbf7d6ca201a2ec717df983af4c339f44ca622911bd3
- fire-752.apk SHA256: 04c692edf0184b3c4b7047ced86fe15d86ad67339427772fa1741b98a19c1470

Queda pendiente extender el arbitraje a otras tareas y aislar los picos de dibujo del catálogo. No se ha medido una mejora global del arranque ni del inicio de reproducción de catchup. Las medidas gfx corresponden a la primera compilación 752; la corrección final del carrusel tiene comprobación visual, no una segunda campaña completa de rendimiento.
