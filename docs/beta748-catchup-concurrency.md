# Investigación de la consulta catchup de 20,8 s
2026-10-16. .185, rama qoder/disneyplus-vod-582. No publicación ni backend modificado.

## Causa y alcance
U7dCatalogStore sincronizaba indexAll/writeAll/write/index/invalidate/toggle
sobre this. SQLiteOpenHelper.getReadableDatabase utiliza ese mismo monitor.
Por tanto una escritura larga impedía incluso abrir la lectura, aunque WAL
estuviera habilitado. Esto explica un mecanismo capaz de producir el pico;
el registro 747 no tenía subfases, por lo que no se puede atribuir retrospectivamente
cada milisegundo de aquella consulta de 20843 ms a ese bloqueo.

Corrección: monitor privado writeLock para serializar escritores, separado del
monitor del helper; transacciones no exclusivas para lectores WAL.
Se mantienen SQL, filtros, permisos, retención, guardados, versión de base y rutas
de reproducción. Sin migración ni borrado de datos.

## Evidencia
708 tests unitarios correctos. Compilación universal release correcta.
En Fire .190, dos pruebas instrumentadas sobre bases temporales:
- helperMonitorIsTheBlockingBoundary: la lectura se bloquea mientras se retiene
  el monitor del helper y continúa al soltarlo.
- readersContinueWhileBatchWriterIsBusy: indexAll queda detenido deliberadamente
  dentro de una transacción; page lee el último estado confirmado antes de
  permitir terminar al escritor. Resultado conservado tras commit.
OK (2 tests), 0,714 s. La app debug de prueba fue sustituida por release beta748
con firma compatible, sin desinstalar ni borrar datos.

## Recorrido real beta748
Directo La 1 visible, portada, catchup, actualización de guía, cambios de día
y segunda página mientras se actualiza. Panel autenticado, exportado y cerrado.
No prueba exhaustiva de todos los proveedores ni reproducción de archivo.

69 consultas de página:
- Acceso helper: mediana/P95/máximo 0 ms (resolución de milisegundos).
- Recuento SQL: mediana 50 ms, P95 282 ms, máximo 1359 ms.
- Lectura excluyendo conversión: 7 / 36 / 45 ms.
- Conversión JSON: 12 / 104 / 951 ms.
- Página total: 78 / 398 / 1516 ms.
- Carrusel: 131 / 560 / 2706 ms.

Referencia 747: página 160 / 647 / 20843 ms. Son sesiones distintas y cachés
distintas, no benchmark controlado. La prueba sintética acredita la corrección
del mecanismo de bloqueo. Los percentiles por fase no se suman.
El recuento SQL y algunos picos de conversión siguen siendo candidatos futuros;
no se han modificado índices ni filtros para esta corrección.

Caja negra: cero eventos perdidos, errores de escritura, CRASH, UI_STALL o
PLAY_ERROR durante el recorrido registrado. ZIP numérico privado, sin sesiones
de panel, URLs ni contenido. No confundir ausencia de esos eventos con garantía
de ausencia de tirones.

## Artefactos
/srv/drbep-dev/artifacts/beta748-catchup
release.apk SHA256 2a33c6fdb77cd63abd333f01c0b196f3131cd8cce70e974447b48169cd8dcd90
fire.apk SHA256 e230952603950cbbc87075f36f6bd31c7d0d129a4866745401bba9886d3847ec
tests.log, build.log, fire-validation.zip.
La APK Fire usa la firma de pruebas preexistente; no publicarla como release.
No commit ni push realizados.
