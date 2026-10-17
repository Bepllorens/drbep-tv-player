# Media3 1.11.1: validación PES al terminar un segmento HLS

## Diagnóstico
En el Fire beta 749 se observaron 102/98 avisos PesReader en capturas de 105 segundos de La 2/BBC Earth. También aparecen en el archivo de Vamos. Los prefijos 0x21... corresponden a bytes PTS. El muestreo de cinco segmentos por sesión comprueba que todos los inicios PES de los PID de vídeo 256 y audio 257 tienen prefijo 000001. Los PID PAT, PMT y SDT no son PES.

En Media3 1.11.1, PesReader consume la cabecera en pesScratch y después reutiliza ese mismo almacenamiento para PTS/DTS. canConsumeSynthesizedEmptyPusi(true), llamado al terminar un segmento, vuelve a ejecutar parseHeader sobre ese almacenamiento. Además del aviso espurio, el rechazo altera payloadSize. No silenciar globalmente el logger ni modificar FFmpeg para ocultarlo.

Fuente revisada: https://github.com/androidx/media/blob/1.11.1/libraries/extractor/src/main/java/androidx/media3/extractor/ts/PesReader.java

## Corrección limitada a beta
La instrumentación de compilación conserva el resultado de parseHeader cuando se procesa una cabecera real y lo consulta al finalizar el segmento. seek invalida el resultado. Se conservan los estados del lector, el caso H262, el comportamiento no-HLS y los avisos por cabeceras realmente inválidas. No se cambian selección DRM, URLs, codecs ni servidor.

El transformador solo actúa sobre PesReader y exige encontrar exactamente una llamada en canConsumeSynthesizedEmptyPusi. Si cambia Media3, revisar y retirar este workaround cuando el upstream esté corregido. El complemento Android sigue en 9.1.0, fijado ahora en buildSrc para compartir la API de instrumentación. Stable no aplica la transformación.

## Regresión
PesReaderEofTest usa el lector real: cabecera válida, almacenamiento sobreescrito por PTS, final de PES sin longitud, PES incompleto con longitud, cabeceras inválidas y estado de cabecera parcial. En el Fire .190, el mismo test contra la beta 749 original falla en la comprobación de EOF (1 de 2); con la corrección pasan los dos tests. Compilación debug y suite JVM completadas. Beta 750 release instalada en el Fire: 90 segundos de catchup La 1, imagen comprobada, cero avisos de prefijo PES, operating-rate, Non-monotonic, FATAL EXCEPTION o Playback error en la captura. También 90 segundos con debug sin esos avisos. Suite JVM: 709 tests sin fallos. El usuario canceló expresamente la repetición de Vamos; no se declara validada en 750. No se ha probado de nuevo retorno al directo ni multipantalla en esta versión.
