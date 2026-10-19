# Refrescos visuales de catchup — beta 757

Desarrollado en .185 e instalado en Fire .190. Sin publicación, commit/push ni cambios de producción. Conserva los cambios pendientes anteriores.

U7dCatalogComposeBinder.kt: ArchiveCardVisual es un modelo inmutable de los campos dibujados. ArchiveCardBody puede omitir recomposición al recibir objetos nuevos con contenido visual idéntico. Acciones actualizadas con rememberUpdatedState, leídas únicamente desde callbacks. ArchiveStatus aísla el contador de progreso. Conserva identidad de programas, slots y foco de 756.

## Validación y límites

721 pruebas superadas y release universal compilada. Seis cambios de día con trazas gfx/view. Las capturas duraron 72,29 s (756) y 53,04 s (757), con distinta actividad de guía: los conteos/sumas no son un benchmark controlado.

| Medida | 756 | 757 |
|---|---:|---:|
| Ejecuciones cardCompose | 844 | 61 |
| cardCompose acumulado | 304,489 ms | 133,334 ms |
| Ejecuciones Recomposer | 312 | 39 |
| Recomposer acumulado | 1789,292 ms | 509,581 ms |
| measureAndLayout acumulado | 644,186 ms | 339,141 ms |
| gfxinfo frames | 132 | 56 |
| gfxinfo p50 | 14 ms | 17 ms |
| gfxinfo p95 | 133 ms | 250 ms |
| gfxinfo p99 | 300 ms | 300 ms |

Hay menos trabajo repetido, pero no mejora demostrada del p95 de transición. Persisten picos. Eliminar frames pequeños cambia la población del percentil; no inferir causalidad solo de porcentajes janky. Pendiente medir latencia por cambio de día con una ventana de captura idéntica y datos estables.

En Fire: navegación tras seis cambios de día; ficha de CSI: Miami de Calle 13 coincide con tarjeta; retorno conserva el foco incluso tras insertarse Colombo por delante durante actualización. Sin prueba exhaustiva de seek/replay en esta iteración visual.

## Artefactos

/srv/drbep-dev/artifacts/card-snapshots-757: trazas privadas, capturas, gfxinfo y APK. Firma existente de pruebas del Fire, instalación sin borrar datos. 589 entradas fuera de META-INF idénticas entre ambas APK.

Release SHA256: e475cbf5ae62dc0e0ae5fbae16946fb35add96154f95057676fc8a032158c9ee
Fire SHA256: a3a335e59e513bde729f1cbe15280a340c9b5b6f5ed1d9e5a256633ab72661e0
