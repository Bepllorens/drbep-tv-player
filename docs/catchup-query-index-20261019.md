# Picos en las primeras consultas de catchup — beta 761

Desarrollado en .185 e instalado en Fire .190. Sin publicar, commit/push ni modificaciones en .223, asociaciones o rutas de reproducción. Conserva todos los cambios pendientes anteriores.

## Diagnóstico

760 conserva el comportamiento de 759 y añade trazas desglosadas: apertura SQLite, recuento, lectura de filas, decodificación, tira, géneros y siguiente emisión. Amplía `ArchivePageTrace`, `U7dCatalogStore`, `MainActivity` y `tools/measure-page-latency.py`; los marcadores contienen solo etapas, identificadores numéricos y tiempos.

En la primera tanda real de doce cambios, COUNT tardó 405–1561 ms; la lectura de las 60 fichas, 8–68 ms, y la decodificación, 6–40 ms. El recuento dominaba. Géneros también mostró 75–500 ms. No era la apertura de SQLite (0 ms en esas muestras).

## Cambio

`U7dCatalogStore` añade `programs_page(scope,day,start DESC,id,channel,end,genre)`. El índice cubre el recuento normal de los programas permitidos de un día, sin tener que cargar el payload de miles de fichas. Conserva los índices anteriores y deja la elección de plan al optimizador.

Schema 3 → 4: migración aditiva dentro de la transacción de SQLiteOpenHelper. No cambia ni elimina programas, catálogo o guardados; tampoco reimporta JSON heredados para esquemas >= 2. Para una eventual reversión de código, conservar soporte de schema 4: una APK antigua que solo admita schema 3 no es una reversión compatible.

No se modificaron la consulta, permisos, filtros, orden, límites, caducidad, red, claves, asociaciones ni rutas de reproducción. 761 incluye la prioridad inmediata de 759.

## Comparación en Fire

Mismo recorrido de doce cambios, repetido una segunda vez por versión. Inicio al ejecutar el botón de día, fin en el primer dibujo de la cuadrícula correspondiente. No mide la latencia física del mando, finalización de imágenes remotas ni el tiempo total desde lanzar la aplicación. La guía continúa actualizándose: datos y carga del sistema no están congelados.

| Métrica | 760 referencia | 761 índice |
|---|---:|---:|
| Primera tanda: mediana total | 1887,45 ms | 701,85 ms |
| Primera tanda: máximo total | 3615,56 ms | 1532,31 ms |
| Primera tanda: mediana COUNT | 774,5 ms | 32,5 ms |
| Primera tanda: rango COUNT | 405–1561 ms | 16–87 ms |
| Repetición: mediana total | 720,06 ms | 733,39 ms |
| Repetición: máximo total | 1186,77 ms | 1102,71 ms |
| Repetición: mediana COUNT | 88,5 ms | 32 ms |
| Repetición: rango COUNT | 35–383 ms | 13–90 ms |

Mejora observada de la primera tanda: mediana total -62,8 %, COUNT -95,8 %. Con caché caliente no se demuestra mejora global adicional: 720 frente a 733 ms es prácticamente igual en esta muestra. No declarar eliminados todos los picos. Persisten costes antes de solicitar la página, en géneros, construcción de modelos y dibujo. Los peores recuentos iniciales sí quedaron corregidos en las muestras medidas.

## Validación

- 721 pruebas Android: cero fallos y errores. Release universal compilada.
- `tools/verify-catchup-page-index.py`: 69.120 registros sintéticos, 24 combinaciones de filtros/guardados/permisos, tres offsets por combinación. Mismos recuentos, IDs, orden y páginas antes/después; catálogo y guardados preservados. DDL del índice leído del Java real, creación repetida idempotente. Confirma plan COVERING INDEX en SQLite de desarrollo; no pretende reproducir los tiempos del Fire. Aumento aproximado del índice en ese fixture: 2,79 MiB.
- Fire: 24 cambios por versión; ficha AMC Crime abierta y retorno a la misma tarjeta enfocada; segunda página 61–120 comprobada mediante pulsación táctil ADB. No se certifica la recuperación de foco tras alternar táctil y mando en paginación: la captura de esa comprobación no mostraba foco.
- Salida del catálogo con Cuatro en directo visible. Acceso por mando a timeshift y carrusel; controles ocultados al terminar. No se probó un nuevo replay ni un seek exhaustivo, pues esta corrección afecta a consultas de catálogo.
- Instalada 761 / 2.0.761-beta-query-index sin desinstalar ni borrar datos. 589 entradas fuera de META-INF coinciden entre APK release y APK con firma existente del Fire.

## Artefactos

Privados: `/srv/drbep-dev/artifacts/query-index-761`, con cuatro capturas de trazas/resultados en `measurements/`, capturas visuales, prueba sintética y APK. Referencia 760 en `/srv/drbep-dev/artifacts/query-trace-760`.

Release761 SHA256: `cebf8b8bc5ebc4c427adbe872a2dc9ee945fde73afab82983aa41db96c3e8b6d`

Fire761 SHA256: `fcdb951e6ec5bf6cf5aed6b0c923e057cf7fb5a2a72b8fffafe21ab0f585676a`
