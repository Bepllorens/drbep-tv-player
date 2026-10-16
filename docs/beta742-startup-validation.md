# Beta 742: depuracion del arranque (2026-10-16)

Entorno: desarrollo y compilacion en .185, Fire AFTKRT ARM32 mediante ADB.
No se publico ni se modifico produccion. Se conservan los cambios pendientes anteriores.

## Cambios de esta iteracion
- U7dChannelPolicy: resolver primero los identificadores conocidos y evitar concatenar metadatos y URLs de todos los canales para reconocer un proveedor. Se mantiene la coincidencia insensible a mayusculas.
- ArchiveAvailability: compilar una sola vez los patrones de identificacion del servicio, manteniendo los patrones y el orden.
- CatalogSnapshotStore: reutilizar la cadena vacia al leer campos de longitud cero. No cambia el formato almacenado.
- Prueba adicional de deteccion de proveedores por metadatos y URL, casos mixtos y entrada nula/vacia.

## Medidas
Dos arranques de proceso con catalogo local existente de 3.048 canales; no son primeras instalaciones ni un benchmark de red controlado.

| Medida | Referencias beta 741 | Beta 742 prueba 1 | Beta 742 prueba 2 |
| --- | --- | --- | --- |
| Aplicar catalogo | 1.067 / 1.116 ms | 614 ms | 499 ms |
| Primer video principal desde prepare | - | 5.907 ms | 3.364 ms |
| Primer video miniatura desde prepare | 9.447 / 7.997 ms | 7.051 ms | 6.034 ms |
| Primer video miniatura desde Activity | - | 8.769 ms | 7.340 ms |

Contadores de miniatura observados: 0 frames descartados hasta 722 frames en la primera prueba y hasta 864 en la segunda. Esto no garantiza ausencia de tirones fuera del intervalo observado.
La segunda prueba incluye carga inicial de manifiestos de aproximadamente 654-658 KB, con duraciones de 1.552 y 943 ms. No se han cambiado las rutas de reproduccion.

## Validacion y limites
- 702 tests, 0 fallos, 0 errores, 0 omitidos.
- Compilacion universal firmada correcta y firmas comprobadas.
- Instalacion -r en Fire correcta, conservando datos.
- Video visible en miniatura de inicio y en pantalla completa.
- La comprobacion adicional de Desde el inicio no permite certificar por si sola catchup/timeshift: la captura inicial mostraba el directo y no se ha establecido una medicion verificable del posicionamiento. No declarar esa regresion como comprobada.
- No se han modificado claves, asociaciones persistidas ni rutas de La 1/Vamos.
- No se publica ni se realiza commit/push en esta tarea.

## Artefactos
Directorio privado de artefactos: /srv/drbep-dev/artifacts/beta742-startup
- release.apk SHA256: 24b0fce4eb5c739bca68e0980a21cba6d287649e1b30830dd435883da0670f53
- fire.apk SHA256: 522f18623c82a10bb1d8ec7d4b52e93173dfbdeaffd0f2ff1f006b07ea2f290a
- build.log, tests.log, tests-final.log.
El APK instalado en Fire conserva su firma de prueba existente; release.apk utiliza la firma oficial. No publicar fire.apk en el canal.
