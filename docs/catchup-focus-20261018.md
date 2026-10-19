# Catchup: coste del foco — beta 753, 18 octubre 2026

Desarrollo en .185. Sin publicación ni cambios en .223. Continúa el bloque de la beta 752; conserva todos sus cambios pendientes.

## Cambio

U7dCatalogComposeBinder.kt: la selección solo se lee al mostrar la ficha. Antes cada cambio de selectedId invalidaba el catálogo completo. Se extrae ArchiveGridCard para aislar su estado de foco y se conservan horario, color del día y género con remember, invalidándolos cuando cambian sus datos. Sin modificar acciones de reproducción, asociaciones, consultas o DRM.

## Validación

713 pruebas unitarias, cero fallos y errores. Release universal compilada. Instalación conservando datos en Fire .190, versión 753. Arranque con miniatura visible, recorrido horizontal, apertura de ficha, retorno a la tarjeta y cambio de día con navegación posterior comprobados visualmente.

Medición con tools/measure-catchup-focus.py, desde primera tarjeta, 16 movimientos (derecha x4, izquierda x4, repetido), 350 ms de espera entre comandos ADB. No mide latencia del mando; gfxinfo mide los fotogramas dibujados.

| Medida | 752 | 753 |
|---|---:|---:|
| Fotogramas | 28 | 51 |
| Janky | 24 (85,71%) | 34 (66,67%) |
| p50 | 31 ms | 20 ms |
| p95 | 53 ms | 38 ms |
| p99 | 57 ms | 57 ms |

Resultado orientativo: diferente actividad de actualización de guía y contenidos entre recorridos. No es un benchmark controlado ni demuestra eliminación de tirones; persisten picos. No se ha repetido una prueba exhaustiva de reproducción/timeshift en esta iteración de presentación. Siguiente foco: publicaciones repetidas del catálogo y carga de imágenes durante actualización.

## Artefactos

/srv/drbep-dev/artifacts/catchup-focus-753 conserva APK, mediciones y capturas.
Release SHA256: 1684b4e2170c68cde7c510c3790a2b7f0cdd6da7b54cfec38e6fc8f0133d96c5
Fire SHA256: 45239c9629d6879be3fe55139e3dddf267721a3e868e7ccfefc7fb7f47051421
Las 589 entradas fuera de META-INF son idénticas. APK Fire usa la firma de pruebas existente, sin desinstalar. APK oficial aún no publicada. Código sin commit/push en esta tarea.
