# Beta 740: espera al volver de VOD y miniatura

Desarrollo en .185; sin despliegue, publicación, modificación de sesiones, claves, asociaciones EPG ni backend.
Conserva los cambios pendientes de 738/739.

Diagnóstico en Fire .190:
- 739: primera miniatura 8,9–10,1 s; picos con búfer de 10–16 s disponible.
- La ruta ISM HLS descarga manifiestos de ~650 KB. El perfil temporal encuentra trabajo en HlsPlaylistParser, String y BigDecimal; no prueba que todo el tiempo HTTP sea de red (el cuerpo se consume durante el parseo).
- Durante arranque se observan GC de decenas de MB y bloqueos de interfaz. La aplicación de catálogo completo tardó 1033 ms en una muestra.
- Reproducción de película a TV, beta diagnóstica 740: ruta /live/1105475 con MIME video/mp2t, sin StreamInfo. Primer intento: 15086 ms y ERROR_CODE_PARSING_CONTAINER_MALFORMED; primer fotograma tras recuperación a 21385 ms.
- El arranque del mismo canal sí usó HLS ISM. La detección exigía formato Smooth en URL/metadatos, pero la URL opaca del catálogo completo no lo conserva. La caché de StreamInfo puede vaciarse por presión de memoria.

Corrección:
- Reconocer directo explícitamente etiquetado Movistar ISM con URL /live del backend cuando falta el formato, conservando la ruta HLS existente. No inferir ISM si hay formato explícito distinto; excluir VOD/catchup del nuevo caso.
- Probar SurfaceView en miniatura de TV para independizar el vídeo del dibujo de Compose. Se conserva TextureView para dispositivos táctiles no protegidos.
- Métricas beta limitadas a fases, duración, tamaños y contador de fotogramas; sin URLs ni cabeceras. Muestreo de CPU y logging HTTP temporal retirados antes de comparar.
- No reducir la ventana de timeshift ni el búfer a ciegas.



Validación Fire de binario final (16/10/2026):
- 698 tests sin fallos; APK instalada mediante -r, sin pérdida de datos.
- APK oficial SHA256 211e7ddbf8e97d3876a1ee574250b50e478c03d61f8785c0513f4bd118ff8f4d.
- Copia Fire con firma compatible SHA256 ae0ae86ee795c5872e2f9fe657adb41d264f78504d5e117ad9f572584ab7efaf.
- Primer minuto miniatura SurfaceView: 4 fotogramas perdidos, máximo consecutivo 1; muestras posteriores sin incremento. Referencias anteriores: picos de 10-15 consecutivos. No prometer cero pérdidas en toda situación.
- Primer fotograma de miniatura tras arranque final: 12,399 s desde prepare (el arranque completo no se ha acelerado en esta muestra).
- Pantalla completa → portada: nueva superficie recibió fotograma en 1,122 s; captura posterior confirma vídeo, sin negro persistente.
- Película → TV con aviso RUNNING_LOW (nivel10, limpia StreamInfo) confirmado: /hls/ism/1105475/index.m3u8; prepare a165ms y primer fotograma a1715ms desde intento, sin reintento ni error. Referencia fallida: 21385ms.
- Timeshift: barra accesible, retroceso30s y vídeo; ventana2h conservada. Capturas locales /private/tmp/drbep740-home.png, drbep740-return-home-settled.png, drbep740-timeshift-seek.png.
- Multiview: añadir La 2 desde el control Añadir pantalla, ambas ventanas muestran vídeo; salir con Atrás recupera La 1 a pantalla completa con imagen. Capturas /private/tmp/drbep740-multiview-final.png y drbep740-multi-exit.png.
