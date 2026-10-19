# Catchup: refresco diferido y arranque (2026-10-17)

## Referencia medida: beta 750, BBC Earth

Evento Diario de los grandes felinos, episodio 5, aun en emision. Ventana de observacion: 15:28:10-15:32:10 CEST. En cuatro minutos no hubo errores de reproduccion, rebuffering, avisos PES ni fotogramas descartados en los contadores observados. Los contadores avanzaron de 75 a 5677 frames, a unos 25 fps. El buffer observado estuvo entre 2,8 y 5,1 segundos.

Se detectaron 178 ciclos de consulta/presentacion del catchup con la interfaz oculta. Las notificaciones incrementales de guia y disponibilidad reconstruian pagina y carrusel aunque no se mostraran.

## Cambio beta 751

DeferredArchiveRefresh conserva solo la ultima solicitud pendiente. MainActivity agrupa solicitudes durante 250 ms y solo las ejecuta si el catalogo o los controles estan visibles. Abrir los controles con mando o tactil reactiva la solicitud pendiente. La comprobacion de generacion impide publicar datos de una sesion anterior. No se detiene la indexacion de la guia ni se modifica la reproduccion, el formato, las rutas o las asociaciones.

Validacion JVM: 711 pruebas, cero errores y cero fallos. Dos pruebas nuevas cubren 178 notificaciones ocultas y las notificaciones que llegan durante una actualizacion. Esto valida el comportamiento de la cola, no demuestra por si solo la mejora en el dispositivo.

## Arranque: desglose observado

- Solicitud del cliente: 15:28:10.
- Manifiesto seleccionado por servidor: 15:28:14.040.
- HLS preparado: 15:28:21.547.
- Primer fotograma: 15:28:21.803.

La mayor espera observada es anterior al reproductor: aproximadamente 4 s de resolucion y 7,5 s de preparacion de segmentos; despues del HLS, unos 256 ms hasta imagen. No se han reducido los buffers del cliente. La ruta conserva dos segmentos completos antes de exponerse. No restaurar el initial burst para eventos en curso: causaba los glitches corregidos. Para optimizar esta fase se necesita una comparacion controlada de disponibilidad de segmentos y margen al directo, no cambiar los umbrales a ciegas.

## Limites

No se ha desplegado backend ni publicado beta/stable. Prueba funcional Fire de 751 pendiente de disponibilidad del mando. Conservar la 750 como referencia de reproduccion estable. No ejecutar pruebas de Vamos: el usuario las cancelo.

## Artefactos verificados

Compilacion universal completada: ARM32 y ARM64. Firma oficial verificada. La copia para Fire conserva las mismas 589 entradas de payload (excluyendo META-INF de firma).

Directorio: /srv/drbep-dev/artifacts/catchup-refresh-751

- release-751.apk SHA-256: 5bea431399920b0a61e0599554f2efdc44c96a27addef7bf61f4f3ab7a4e420f
- fire-751.apk SHA-256: 29b4450ba0408d5b707e8559c08fb49909863a56314ce669d91df70774570490

El Fire responde mediante tunel SSH por el .185 y esta reproduciendo DAZN. Se ha solicitado disponibilidad del mando antes de interrumpir esa reproduccion. No se ha instalado aun la 751.

## Validacion Fire completada (registros recuperados 2026-10-18)

Beta 751 instalada conservando datos. Prueba del 17 de octubre, 21:32:07-21:36:20 CEST: BBC Earth, Especiales de Fauna letal - Episodio 10, evento en emision. Capturas confirman video en movimiento y navegacion del carrusel: OK muestra controles, Arriba llega a la barra y otra pulsacion arriba al carrusel; izquierda mueve el foco al episodio anterior.

Caja negra: 3 consultas PAGE/STRIP iniciales (21:32:11, 21:32:18 y 21:32:24), ninguna entre 21:32:37 y 21:36:20 con controles ocultos. En esta ejecucion la guia ya habia terminado de actualizar antes de reproducir: el resultado es positivo, pero no constituye una comparacion A/B bajo identica carga frente a los 178 ciclos anteriores.

VIDEO_COUNTERS: 127 a 5728 frames en 224 segundos, aproximadamente 25 fps; cero descartados en las ocho muestras; buffer 4178-6953 ms. Memoria Java observada: aproximadamente 74-88 MiB. Sin rebuffering, errores de reproduccion, cierres ni avisos Unexpected start code prefix u operating-rate durante la ventana. Primer frame: 14252 ms. La latencia inicial sigue pendiente y no se atribuye mejora a este cambio.

Datos de prueba privados en el Mac: /private/tmp/bbc751-check/blackbox.zip y logcat.txt; no incorporar los registros brutos a Git. La captura continua de logcat se detuvo al retomar el trabajo. Backend y canales beta/stable sin modificar. Esta validacion sustituye el estado pendiente descrito arriba.
