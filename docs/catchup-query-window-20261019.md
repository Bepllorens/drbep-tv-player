# Catchup: lectura de la ventana de días — beta 762

## Cambio acotado
Tras el índice de la beta 761, la carga auxiliar de la ventana aún leía y decodificaba toda la semana de cada canal antes de filtrar hoy, ayer y el día seleccionado. Se sustituye únicamente esa lectura en loadArchiveCatalog por U7dCatalogStore.readDays: el filtro de días se aplica en SQLite antes de obtener y decodificar los payloads.

No cambia el esquema (continúa en 4), la paginación, la retención, los guardados, las asociaciones, los permisos ni la reproducción. El resto de llamadas a read conservan su comportamiento. Se mantiene ORDER BY start. Usa el día ya indexado, igual que la página: no recalcula la zona horaria de cada payload al leer.

SQLite decide el índice: en la prueba sintética elige programs_strip para preservar orden y filtra el día; no se fuerza programs_channel. La reducción comprobada es de payloads devueltos/decodificados, no se afirma que nunca recorra otras filas del canal.

## Validación
- 721 pruebas unitarias: 0 fallos, 0 errores.
- tools/verify-catchup-window.py: 45 casos sintéticos de cuenta, canal, ventana vacía, días ausentes y orden equivalentes al filtrado previo.
- Ejemplo sintético: 384 programas de ocho días frente a 144 de una ventana de tres días.
- 589 entradas del APK fuera de META-INF idénticas entre el release y la copia firmada para el Fire.
- Instalación con conservación de datos en AFTKRT, versión 762 confirmada.

## Artefactos privados en .185
/srv/drbep-dev/artifacts/query-window-762/

Release SHA-256: 2c30999775453a488741ca8577c46d2a12c91e4070df567743374cebd675fb4e
Fire SHA-256: f04e144d41d62ba781e8c4dbc2c3533de72b461de1faff1afd42b1523eb3f46a

## Alcance
No publicado, sin commit/push en esta iteración y sin modificar producción.
Medición de navegación mediante tools/measure-page-latency.py: desde selección del día hasta dibujo Compose de la página publicada. No equivale a todas las imágenes descargadas ni a latencia física del mando.
La guía se actualiza normalmente durante las pruebas; no es un benchmark con base congelada.

## Medición en Fire
Doce cambios de día por versión tras volver a abrir la aplicación:
| Tramo | 761 referencia | 762 |
|---|---:|---:|
| Mediana selección a dibujo | 753,0 ms | 733,8 ms |
| Máximo observado | 1885,68 ms | 1658,72 ms |
| Mediana selección a solicitud | 303,4 ms | 299,5 ms |
| Mediana consulta de géneros | 89,5 ms | 84,9 ms |
| Mediana construcción de modelos | 68,7 ms | 72,0 ms |

La diferencia típica es pequeña y una sola tanda no establece significación estadística. El ahorro de decodificación está comprobado; no se declara resuelto el pico anterior a la solicitud ni el coste de géneros/modelos.

Comprobación visual por ADB: entrada desde los días a tarjetas, apertura de ficha Henry Danger, retorno conservando el foco del mismo programa aunque el refresco lo desplazó a segunda posición, cierre del catálogo y directo Cuatro visible, acceso a barra. No se inició un replay ni se ejecutó un seek en esta iteración.
