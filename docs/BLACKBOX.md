# Caja negra local (beta 746)

Implementada en el cliente Android. No requiere cambios ni envios al backend.
Desarrollo: /srv/drbep-dev/repos/drbep-tv-player en .185.

## Uso
Ajustes -> Listas personales -> Panel web del dispositivo -> conectar con PIN -> Diagnostico -> Caja negra.
- Actualizar: resumen de tiempos y ultimos 200 eventos.
- Marcar problema: conserva el contexto anterior disponible y vuelve a guardar el contexto a los 30 segundos. Esperar ese tiempo antes de exportar.
- Exportar diagnostico ZIP: descarga archivos JSONL y un README del esquema.
- Detalle durante 10 minutos: muestras de memoria y video cada 5 segundos, en lugar de 30.
- Pausar/activar registro: preferencia persistente. Pausar no borra el historial.

## Diseno y limites
- Singleton por proceso. Datos privados en getNoBackupFilesDir()/blackbox, excluidos del backup de Android.
- Solo enums y campos numericos: sin nombres de canales, titulos, IDs de contenido, URLs, cabeceras, tokens o claves. No se captura logcat ni mensajes de excepciones.
- session identifica el proceso, op el intento de reproduccion; at es epoch UTC y elapsed reloj monotono.
- Cuatro segmentos de hasta 512 KiB y dos instantaneas de hasta 2 MiB: maximo 6 MiB.
- Cola de 512 eventos, drenada en un ejecutor cada segundo; productores sin I/O. Cola llena descarta eventos y lo indica en el resumen.
- Muestreo de heap y contadores de video 30 s / detalle 5 s. Monitor de UI cada 2 s, con un solo heartbeat pendiente. Se suspende en segundo plano.
- Marcadores conservan las dos ultimas instantaneas. Si se marca varias veces seguidas, la ultima marca recoge el tramo posterior; las anteriores conservan su foto inicial.
- Un cierre brusco puede perder el ultimo segundo aun no escrito. El manejador de excepciones intenta un vaciado limitado a 250 ms y encadena el manejador previo.
- En Android 11+ recoge motivo/fecha/importancia de las cinco ultimas salidas si el sistema las ofrece, sin trazas ni descripcion. No sustituye todas las herramientas de ANR.
- P50/P95/max del panel se calculan sobre las ultimas 256 muestras por evento presentes en el anillo, mezclando sesiones; el contador es de todos los eventos retenidos. No confundirlo con un benchmark ni una sesion concreta.
- Exportacion y resumen usan el hilo de escritura, fuera del hilo UI, con espera limitada del endpoint. ZIP acotado por los archivos retenidos.
- Rutas /blackbox y /blackbox-export solo despues de autenticacion del panel; se conservan comprobaciones de Host, Origin y caducidad.

## Cobertura inicial
Arranque, hitos de ventana/reproductor/catalogo, coste del modelo de Inicio, navegacion de Inicio/secciones y mando, preparacion/estado/errores/frames del reproductor, contadores de video, memoria, bloqueos del hilo principal y cierres.
No se registran aun latencias individuales de red/EPG/catchup ni las ventanas de multipantalla. No se han cambiado rutas ni claves de reproduccion.

## Archivos
- BlackBoxStore.java: cola, rotacion, instantaneas, resumen y exportacion.
- BlackBox.java: ciclo de vida, memoria, heartbeat y excepciones.
- MainActivity.java y PlayerController.java: eventos.
- DeviceWebPanel.java y assets/device-panel.html: endpoints e interfaz.
- BlackBoxStoreTest.java y DeviceWebPanelTest.java: persistencia, limites, corrupcion, pausa y autenticacion.

SURFACE_FRAME mide desde el cambio de superficie; PLAY_FRAME mide desde prepare. El resumen omite los PLAY_FRAME repetidos del prototipo 745.


## Ampliación 747
EPG_LOAD mide la obtención de pares actual/siguiente. GUIDE_LOAD mide cada lote
de guía oficial, incluida espera por exclusión mutua: a=ms, b=filas,
c=0 completo/1 excepción/2 parcial. GUIDE_CACHE: a=bloques vigentes,
b=bloques ausentes o caducados, c=filas disponibles (incluidas caducadas).
GUIDE_HTTP: a=tiempo hasta cabeceras, b=HTTP, c=bloques solicitados.
GUIDE_PARSE: a=lectura y parseo ms, b=filas. GUIDE_FALLBACK:
a=tiempo de consulta, b=1 si se conserva caché, c=1 fallo/2 hilo interrumpido.
Estos eventos comparten op por lote, sin códigos de canal ni fechas solicitadas.
CATCHUP_PAGE y CATCHUP_STRIP miden consultas paginadas y carrusel en SQLite;
a=ms, b=filas devueltas, c=0 éxito/1 excepción. No realizan consultas adicionales.

MULTI_* usa op como identificador temporal único del reproductor secundario,
no como canal. OPEN a=rol; CLOSE cierra su vida. PREPARE a=intento,b=rol;
STATE a=estado Media3,b=intento; FRAME a=ms desde preparación,b=intento,c=rol;
ERROR a=código Media3,b=intento. COUNTERS a=frames,b=descartados,c=búfer ms.
Se aprovecha el watchdog existente, con muestreo normal 30s/detalle 5s;
no hay temporizadores nuevos por ventana. La ventana puede reutilizarse
para distintos canales y se distingue cada intento numérico.
Las mediciones son internas, no prueban por sí solas ausencia de tirones.

## Investigación 748
CATCHUP_DB_OPEN mide acceso al helper (incluida espera), CATCHUP_DB_COUNT el recuento SQL, CATCHUP_DB_FETCH la lectura de filas excluyendo decode y CATCHUP_DB_DECODE la conversión JSON. Comparten op con CATCHUP_PAGE. a es ms y b el número de resultados cuando aplica. No se cambian los filtros ni el esquema. Las escrituras usan un monitor propio y transacciones no exclusivas para permitir lectores WAL.
