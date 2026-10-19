# Latencia visible al cambiar de día — beta 759

Desarrollo y compilación en .185. Instalada en Fire .190, conservando datos. Sin publicación, commit/push ni cambios en producción, asociaciones, claves o rutas de reproducción.

## Medición

La beta 758 añade únicamente instrumentación sobre 757. `ArchivePageTrace.java` emite secciones atrace con identificadores numéricos, sin títulos, URLs o sesiones. Inicio: ejecución del botón de día. Fin: primer `drawContent` de la cuadrícula que ha compuesto el día publicado. No mide latencia física del mando, presentación final de SurfaceFlinger ni terminación de todas las imágenes remotas. `publishedDay` queda capturado en la composición para no atribuir a datos nuevos el dibujo de datos antiguos.

`tools/measure-page-latency.py` se ejecuta desde el Mac, con el Fire en Catchup, Hoy seleccionado y enfocado. Arranca y detiene atrace dentro de la misma ejecución; repite derecha/derecha/derecha/izquierda/izquierda/izquierda dos veces, confirmando cada día y dejando tres segundos entre acciones. Los resultados usan el reloj del dispositivo, excluyendo esperas y transporte ADB. Conservar trazas fuera de Git.

Se realizaron dos tandas de doce cambios por versión. Los días anteriores estaban precargados. Se mantuvo el comportamiento normal de la guía: continúa actualizando datos y Hoy cambia con el reloj. Es una comparación en el dispositivo real, no un benchmark con una base congelada ni una prueba de descarga fría.

## Corrección

En `MainActivity.java`, `queryArchivePage` publica su petición explícita con prioridad inmediata. Los refrescos automáticos siguen pasando por `DeferredArchiveRefresh` y sus 250 ms de agrupación. La entrega inmediata reemplaza el trabajo pendiente y cancela su temporizador, evitando que este drene un lote posterior antes de tiempo. Se mantienen las guardas de generación, revisión, filtros y visibilidad.

No se alteran consultas SQL, caducidad, red, ventanas de archivo ni controles de reproducción.

## Resultados (milisegundos)

| Tanda / métrica | 758 referencia | 759 corrección |
|---|---:|---:|
| Primera tanda: mediana total | 933,4 | 795,9 |
| Primera tanda: máximo total | 1822,2 | 2190,8 |
| Repetición: mediana total | 839,4 | 648,5 |
| Repetición: máximo total | 1155,3 | 1036,4 |
| Repetición: petición → programación, mediana | 262,5 | 11,7 |
| Repetición: consulta y preparación de datos, mediana | 135,0 | 197,6 |
| Repetición: publicación → dibujo, mediana | 86,0 | 92,1 |

La mediana total en la repetición baja un 22,7 %. La mejora demostrada es eliminar la espera de agrupación para una acción explícita. No han desaparecido los picos: la primera tanda de 759 tuvo un máximo mayor, con aproximadamente un segundo preparando datos en una consulta. El tramo previo a solicitar la página conserva unos 254 ms de mediana; la actualización de guía iniciada al cambiar de día sigue siendo candidata a instrumentación adicional. No atribuir toda esa demora a SQLite sin separar las consultas concretas.

## Validación

- 721 tests Android, cero fallos/errores; build universal release correcto.
- 24 cambios de día medidos por versión.
- Ficha de Gumball abierta y retorno a la misma tarjeta con foco, incluso al insertarse programas nuevos por delante.
- Salida de catálogo con directo de Cuatro visible. Acceso con mando a timeshift y a la tira de catchup; vuelta al directo sin cambiar de canal. No se ejecutó un replay ni seek exhaustivo en esta iteración de presentación.
- APK instalada: 759 / 2.0.759-beta-page-priority.
- 589 entradas fuera de META-INF idénticas entre release y APK firmada con el certificado existente del Fire.

Artefactos privados: `/srv/drbep-dev/artifacts/page-priority-759`, con las cuatro tandas en `measurements/`, capturas y APK. La referencia está también en `/srv/drbep-dev/artifacts/page-latency-758`.

SHA256 release759: `2d006ba08addbbe87984cedace0e1dec201698e66a15bdf3eefacc4fc9f639f2`

SHA256 Fire759: `3d21fa913e8c06cd368d457acadc3fd12afb3f9a2c37ae0b2dfafed3f836f6c4`
