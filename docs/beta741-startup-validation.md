# Beta 741: primera carga de Inicio
Desarrollo en .185, prueba autorizada en Fire .190. Sin publicación ni cambios de producción.
Se conservan las correcciones 738–740.

## Referencia y diagnóstico
- Beta 740, arranque de proceso: primer fotograma principal 8689 ms desde prepare; miniatura 15457 ms. Hilo UI con pausas de 73–159 fotogramas.
- Otro arranque instrumentado: principal 7839 ms y miniatura 12649 ms.
- Muestreo temporal de UI a 125 ms (retirado antes del binario final): repetición de CatalogSnapshotStore.getStatus/getAccessToken/decryptPreferenceValue; búsqueda vodOriginLogo/matchesVodVisualPlatform domina parte de la creación de Inicio.
- Modelo inicial medido 1812 ms; modelos posteriores 189–401 ms. El muestreo afecta al coste: comparar latencia final también sin él.
- No se ha cambiado la ruta ni la ventana HLS/timeshift ni el formato de reproducción.

## Cambio
- Credencial local de DRBEP descifrada una vez por valor cifrado y por instancia, solo en memoria. Cada lectura comprueba la preferencia actual; reset explícito al escribir/borrar; errores no conservan el valor anterior. Persistencia sigue cifrada.
- Logo de proveedor memoizado, incluyendo ausencia; invalidación conjunta con las cachés derivadas del catálogo. Descarta candidatos sin logo antes de clasificar su proveedor.
- Clasificación VOD retorna en cuanto conoce el proveedor solicitado; mismas reglas.
- Retirada la construcción de la tira antigua de proveedores VOD sin consumidores en Inicio. Los selectores VOD actuales siguen resolviendo sus logos al abrirse.
- Se evita reconstruir inmediatamente el modelo ya enlazado; se conserva la solicitud inicial de EPG.
- Métrica beta de tiempo del modelo, sin datos sensibles. Muestreador temporal retirado.
- 701 tests, sin fallos, errores ni omitidos.

## Validación final
Primera iteración (antes de retirar la tira VOD y refresco redundantes): principal 6688 ms, miniatura 12506 ms y modelo inicial 1750 ms. No se consideró suficiente y se continuó optimizando.
Revisión final instalada con -r y datos conservados.
- Arranque 1: modelo inicial 518 ms; fotograma principal 5763 ms y miniatura 9447 ms desde prepare. Desde Activity.onCreate hasta miniatura: 11961 ms.
- Arranque 2 (force-stop, datos conservados): modelo inicial 734 ms; principal 5965 ms y miniatura 7997 ms desde prepare. Desde Activity.onCreate hasta miniatura: 9726 ms.
- Primer arranque: 5 fotogramas perdidos, máximo consecutivo 2, sin incremento en muestras posteriores hasta rendered=892. Segundo: 4 perdidos en la muestra rendered=517, máximo consecutivo 2. No afirmar cero tirones.
- Portada con vídeo, logos y EPG comprobada en captura; cambio de programa EPG a las 10:35 reflejado.
- Pantalla completa -> portada: superficie enlazada 10:36:41.880 y fotograma 10:36:43.275 (1395 ms); captura confirma imagen.
- Zapeo La 1 -> La 2 -> La 1 con imagen; primeros fotogramas 1314/1474 ms desde intento, sin cierres observados.
- No se han cambiado superficies, timeshift, catchup ni multiview en esta revisión. Sus pruebas completas previas corresponden a 740; no presentar como repetidas en 741.
- Capturas locales: /private/tmp/drbep741-home-final.png, /private/tmp/drbep741-full.png, /private/tmp/drbep741-return.png, /private/tmp/drbep741-tv.png (La 2).
- APK oficial universal SHA256: 155f50504e30292b9e174b30b5b27dba8960695b1a2457fc2a9c48bdf528d7e6.
- APK Fire con firma compatible SHA256: 5ab9c257c9aadfd160b2e7efb57bb9092f13909df9e55312fd4419d1939d0036.
- Persisten costes de descarga/parseo HLS y aplicación inicial del catálogo (~1,1 s). El arranque no es instantáneo.
- No publicada. No commit/push solicitado en esta tarea.
