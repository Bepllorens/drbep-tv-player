# Catchup: reconstrucción y disposición — beta 756

Trabajo en .185, probado en Fire .190. No publicado, sin commit/push ni cambios en .223. Conserva cambios pendientes de 751–754. La beta 755 fue una iteración instrumentada; la instalada al terminar es 756.

## Evidencia y corrección

Atrace gfx/view con etiquetas Archive.cardCompose, Archive.imageBind y Archive.imageCreate. La creación de ImageView no era el coste principal: en 755 consumía 4,1 ms acumulados. AndroidOwner:measureAndLayout alcanzó 188,9 ms en un pase.

ArchiveGridSlots conserva las celdas entre filtros/días/páginas, usando la identidad de la página publicada, no del filtro pendiente. Dentro de una misma página los keys siguen al ID de programa al reordenarse o insertarse eventos. No se reutiliza la identidad de un programa para otro durante un refresco. AndroidView reutiliza su vista, cancela la petición anterior y elimina tag/drawable antes de enlazar otro programa; onReset/onRelease limpian también. Se conserva el aspecto de imagen y la lógica de reproducción.

Archivos: U7dCatalogComposeBinder.kt, U7dCatalogState.kt, ArchiveGridSlots.java, ArchiveGridSlotsTest.java, MainActivity.java, app/build.gradle. Analizador: tools/analyze-archive-trace.py. Los Log.i ArchiveCost de MainActivity no aparecen en el release (probable eliminación por optimización); no hay medición válida por ese mecanismo de query/modelos. No atribuirles cifras.

## Medición en dispositivo

Seis cambios de día con tools/measure-catchup-navigation.py, trazas activas en ambos recorridos. La actualización de guía y contenidos varía: comparación orientativa, no benchmark controlado. Las sumas corresponden a las capturas completas, que pueden tener diferente actividad/duración; priorizar distribución y conteo de vistas.

| Bloque | 755 | 756 |
|---|---:|---:|
| ImageView creadas | 60 | 1 |
| measureAndLayout máximo | 188,881 ms | 88,038 ms |
| measureAndLayout p95 | 131,249 ms | 31,307 ms |
| measureAndLayout acumulado | 945,001 ms | 581,675 ms |
| Cuerpo tarjeta mediana | 0,395 ms | 0,163 ms |
| imageBind mediana | 1,326 ms | 1,393 ms |
| Recomposer acumulado | 508,433 ms | 1296,379 ms |
| gfxinfo p90 | 250 ms | 150 ms |
| gfxinfo p95 | 250 ms | 250 ms |
| gfxinfo p99 | 300 ms | 450 ms |

Conclusión parcial: reducción comprobada de creación y coste de disposición; persisten fotogramas lentos y el p99 global empeora en esta muestra. No declarar resueltos todos los tirones. Investigar publicaciones/recomposiciones durante actualización de la guía, con una comparación de datos estables.

## Validación

721 tests, cero fallos/errores, release universal compilada. Pruebas específicas de cambio de día, reordenación dentro de página, inserción, lista vacía y crecimiento. En Fire: seis cambios de día; imágenes coherentes en las tarjetas revisadas; desplazamiento; ficha La maldición de Oak Island y retorno al mismo programa; directo DAZN visible; acceso por dos pulsaciones arriba hasta la tira de catchup pasando por timeshift. No se ha ejecutado seek ni reproducción exhaustiva de archivos de todos los canales.

## Artefactos

/srv/drbep-dev/artifacts/grid-slots-756: capturas, trazas privadas, gfxinfo y APK. Firma Fire de pruebas existente; instalación -r sin borrar datos.
Release SHA256: 8143eb93a075d0a8412ce21dd1082c24b5780769fb2cdd93c84bce16235bd9cc
Fire SHA256: 0c9d7f9ade609ad25ed0eaa3a87d71fe9997121d949bb4027b870f06f9e5b49b
589 entradas fuera de META-INF idénticas entre ambas APK. VersionCode 756.
