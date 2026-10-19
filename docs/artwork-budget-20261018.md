# Precargas de imágenes — beta 754

Desarrollado en .185, instalado en Fire .190 conservando datos. No publicado, sin commit/push ni modificación de producción.

## Corrección

Cada publicación del catálogo lanzaba hasta doce precargas oficiales repetidas. ArchivePreloadBudget limita cada URL a un intento especulativo por minuto, con 256 entradas como máximo y expulsión de la más antigua. Las cargas de las tarjetas visibles no pasan por este filtro; no bloquea reintentos visibles. Las nuevas URLs entran inmediatamente. Las precargas usan fitCenter, dimensiones 320x180 dp y AUTOMATIC como el enlazado habitual de las imágenes oficiales.

Archivos: MainActivity.java, ArchivePreloadBudget.java, ArchivePreloadBudgetTest.java, app/build.gradle. Se conservan los cambios previos pendientes.

## Validación

717 tests, cero fallos/errores. Cuatro nuevos verifican repetición, caducidad, cambio de URL, límite de capacidad y reinicio de reloj. Release universal compilada. Verificadas imágenes visibles, seis cambios de día y navegación con foco en el Fire.

Comparación orientativa con tools/measure-catchup-navigation.py (seis cambios de día, datos/imágenes y actualización de guía variables):

| Métrica | 753 | 754 |
|---|---:|---:|
| Frames | 74 | 77 |
| Janky | 25 (33,78%) | 34 (44,16%) |
| p50 | 12 ms | 12 ms |
| p95 | 250 ms | 300 ms |
| p99 | 350 ms | 450 ms |

No demuestra mejora del cambio de día; no declarar eliminados los tirones. La deduplicación de precargas está comprobada por tests, pero no identifica la causa principal de los picos. Pendiente separar mediante trazas el coste de reconstruir/componer las tarjetas de la carga y entrega de imágenes. No se ha repetido la prueba exhaustiva de reproducción/timeshift en esta iteración.

## Artefactos

/srv/drbep-dev/artifacts/artwork-budget-754 contiene capturas, gfxinfo y APK.
Release SHA256: 51fec86863598846517349dcf8bdd0b3b8349b55c1e05977b75b3dfc71b99ba9
Fire SHA256: 3b25d0309786bf207e56c056561ed0eddc42911aa2afb3b5accaf05af0e2ae4a
589 entradas fuera de META-INF idénticas entre APK oficial y APK con la firma existente de pruebas del Fire. VersionCode 754.
