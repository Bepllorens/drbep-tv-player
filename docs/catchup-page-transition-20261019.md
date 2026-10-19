# Catchup: transición y consultas, beta 768

## Referencia 766
Doce cambios de día en Fire: mediana 558,8 ms, máximo 876,68 ms.
La espera del ejecutor llegó a 395,6 ms; la consulta de géneros costó 68–144 ms
por página. Los modelos llegaron a 204 ms y el dibujo a 152 ms.

## Experimento 767 descartado
Una consulta recursiva de géneros dio resultados exactos y rápidos en SQLite de
desarrollo con 120.000 filas sintéticas. En el Fire llegó a bloquear la consulta
16 segundos. No se publica ni se recomienda usar 767: su consulta se ha retirado.
Además, cancelar por cualquier revisión de fondo podía impedir terminar páginas.
La validación en dispositivo detectó ambas limitaciones antes de publicar.

## Implementación 768
- Se conserva SELECT DISTINCT compatible con el Fire. Solo su pequeña lista de
  géneros se reutiliza por cuenta durante 15 segundos. No se cachean las páginas
  ni se retrasa la actualización de programas. Las listas vacías no se retienen.
  Un género nuevo puede tardar hasta 15 segundos en aparecer en el selector.
- Cancelación cooperativa tras cada consulta SQL y entre tarjetas únicamente
  cuando llega una nueva petición de página, no por un lote del mismo filtro.
- Identidad completa de página: día, canal, género, búsqueda, guardados y offset.
  Hasta recibir la solicitada, las tarjetas anteriores quedan ocultas y no
  pueden abrirse. Se conserva la cuadrícula para reutilizar componentes.
- Contador y paginación antiguos ocultos durante la transición; indicador sin
  foco. Los refrescos del mismo filtro no vacían la parrilla.
- La traza de dibujo exige publicación previa de la misma petición, evitando
  medir un repintado anterior al volver rápidamente a un día ya visible.

## Validación
Pruebas de identidad (incluida puntuación en filtros), caducidad y aislamiento
de caché, listas vacías y conservación de instantáneas. Pendiente prueba Fire.
Sin migración de base de datos, cambios de reproducción ni producción.

## Resultado en Fire 768
- 731 pruebas automatizadas, cero errores/fallos; instalación confirmada.
- Dos tandas de 12 cambios completadas: mediana/máximo 611,34/764,68 ms y
  492,995/873,65 ms. Referencia 766: 558,8/876,68 ms. Datos de guía cambiantes;
  no se afirma mejora global sostenida ni desaparición de todos los picos.
- Cola del ejecutor: máximo 118,77 y 90,72 ms frente a 395,6 ms de la referencia.
  Consulta de géneros reutilizada: mediana 0,03 ms en ambas tandas. Al caducar,
  aún alcanza 325/293 ms; modelos y dibujo siguen siendo variables.
- Prueba visual grabada fuera de la medición: transición con mensaje de carga
  y sin tarjetas de la página anterior. Las imágenes de la nueva página siguen
  cargándose progresivamente. El vídeo de validación no se usa para tiempos.
- Mando: bajar desde días a tarjetas, derecha entre tarjetas y subir a días,
  correcto. Cambios rápidos y regreso al reproductor comprobados.
- Cuatro restaurado y reproduciendo; acceso al foco de timeshift y luego al
  carrusel comprobado. No se ejecutó seek ni replay en esta tanda.
- APK release SHA256 f28080347efe9d5eba5cd204a6032da75210210ad8e02faa80910c8e4b6cbb46.
  Fire SHA256 535c6c1ed1b4fd67e4ba20c22f756900321f7b7439d1093c7e79de5702171470.
  589 entradas idénticas fuera de META-INF. La beta 767 es experimental descartada.
- Evidencia privada: /srv/drbep-dev/artifacts/catchup-pages-768/.
- Sin publicación, commit/push ni modificación de producción. El siguiente
  trabajo medible es preparación/dibujo de tarjetas y renovación de géneros.

## Beta 769: preparación de tarjetas (validada en Fire)

- Datos comunes de cada canal calculados una vez por lote: disponibilidad,
  nombre, proveedor y origen de metadatos. Se mantienen los fallbacks de cada
  programa y las comprobaciones de generación/selección.
- Horario de tarjeta preparado en el ejecutor de presentación, con un solo
  formateador por lote; la composición no crea dos formateadores por tarjeta.
  Se conserva un fallback para entradas construidas por otros consumidores.
- Sin cambios de esquema, consultas, caché de géneros, reproducción o servidor.
- 731 tests unitarios, cero fallos/errores. Compilación universal beta correcta.
- Release SHA256: 9f4a6397b3060f67c829c6496ebd36336696f9880aaa6b594538decbe8fa9433.
- Fire SHA256: 8e995129143ed383c50b591c3290ad18712987f8323f42e3b58ffeba902fc591.
- 589 entradas de contenido idénticas entre release y APK con firma del Fire.
- Evidencia privada: /srv/drbep-dev/artifacts/catchup-cards-769/.

### Comparación contemporánea, 19 de octubre

Dos tandas de 12 cambios por versión, misma secuencia y Fire. La guía sigue
actualizándose: no son datos estáticos. Tiempos de selección a dibujo de página,
no hasta completar las imágenes.

| Tanda | Total mediana/máximo ms | Modelos mediana/máximo ms | Dibujo mediana/máximo ms |
|---|---|---|---|
| 768 control | 578,65 / 948,31 | 128,53 / 236,98 | 123,67 / 256,72 |
| 768 control 2 | 445,73 / 605,90 | 96,81 / 223,46 | 92,57 / 149,69 |
| 769 | 480,44 / 744,90 | 70,59 / 139,50 | 117,56 / 231,70 |
| 769 repetición | 450,45 / 595,63 | 80,91 / 211,06 | 82,89 / 102,38 |

La preparación mejora en ambas tandas; el total de la segunda queda
prácticamente igual. No afirmar eliminación de todos los tirones. Renovar
géneros aún alcanza 393,96 / 251,68 ms en 769 (434,24 / 83,52 en controles).

Validación visual: beta 769 visible, Cuatro reproduciendo en miniatura,
foco de arranque sobre ella; entrada a tarjetas y derecha al segundo elemento;
subida a filtros; regreso a Cuatro a pantalla completa; foco de barra de
timeshift y segunda pulsación arriba al carrusel. No se ejecutó seek/replay
en esta tanda. Registro reciente AndroidRuntime sin errores fatales.
Captura 769-days muestra el filtro Géneros enfocado, no la fila de días.
Sin publicar, commit/push ni cambios de producción. Próximo punto: separar
la renovación de géneros del camino crítico, conservando actualización y
aislamiento por cuenta.

## Beta 770: renovación independiente de géneros

- El escaneo DISTINCT conserva la consulta y los índices existentes, pero pasa
  a un ejecutor propio de un hilo, con una sola renovación pendiente.
- Mientras caduca la caché de 15 s se usa su lista anterior, únicamente dentro
  del mismo scope. El primer acceso muestra la página sin esperar los géneros.
- La entrega actualiza solo el estado de filtros si cambió, sin reconstruir
  páginas. Ya no se entrega una lista de géneros antigua junto a cada página.
- Respuestas de generaciones/cuentas anteriores se descartan y se solicita la
  cuenta vigente si el catálogo sigue abierto. Cambio de scope limpia géneros.
- Al cerrar la pantalla no se programan renovaciones; un trabajo ya iniciado
  puede terminar, sin actualizar la pantalla oculta. Sin temporizador nuevo.
- Al destruir Activity se cierra el ejecutor. No se modifica la reproducción,
  las claves, URLs, esquema de base de datos ni producción.
- Control 769 contemporáneo: 12 cambios, total mediana 438,245 / máximo 887,43
  ms; espera de géneros máximo 383,68 ms.
- 770 en Fire: 12 + 12 cambios. Total mediana/máximo 511,29/1020,43
  y 486,405/661,76 ms. Espera directa de géneros 0,025/0,03 y 0,02/2,3 ms.
  El pico de 1020 ms es 500 ms de página + 162 ms de cola + modelos/dibujo,
  no espera directa de géneros. No afirmar mejora global con esta variante.
- Géneros visibles y filtro Acción comprobado (47 programas); restablecido Todos.
- 732 tests sin errores. APK 770 conservado como variante intermedia.
- Release 770 SHA256 39ceceb690c81812112aaaf0a53567bab2b62a6c7af2098368280dfe17d6b732.
- Fire 770 SHA256 0f7e073dc38b0d4a12edbaa30ca3336e2c97019a8f4a3eb6eaee223d7361a373.

### Beta 771: prioridad de fondo
Mismo comportamiento que 770, con THREAD_PRIORITY_BACKGROUND para el ejecutor
de géneros y sección atrace Archive.genresRefresh para medir la consulta real.
La renovación puede tardar más mientras cede recursos; no bloquea la página.
Validación final en Fire:
- 732 tests correctos, compilación universal beta y diff --check correctos.
- 12 + 12 cambios: total mediana/máximo 528,305/823,01 y 522,475/590,19 ms.
  Espera directa de géneros mediana/máximo 0,02/0,04 y 0,02/0,02 ms.
- Consulta de géneros real, traza independiente: 1510,71, 381,79, 1004,38 ms
  en la primera tanda; 234,68, 782,42, 669,86, 354,15 ms en la segunda.
  El scan sigue teniendo coste y puede competir por recursos. Se elimina su
  dependencia en la entrega de páginas, no se demuestra mejora de mediana total
  frente al control 769 (438,245 ms). La guía/datos siguen variando durante las
  pruebas. No afirmar eliminación de todos los tirones.
- UI: arranque con miniatura y foco; navegación de días durante las mediciones;
  selector de géneros, Acción (47 programas Hoy y 65 Ayer), filtro conservado
  tras expirar la caché y cambiar de día; restaurado Todos/Hoy.
- Regreso al directo Cuatro, foco de timeshift y luego carrusel comprobado.
  No se ejecutó seek ni replay; no se cambiaron canales, URLs, claves o servidor.
  La captura de cuadrícula posterior a toques no confirma foco sobre tarjetas;
  no se usa como prueba de ese recorrido específico.
- Registro AndroidRuntime reciente sin errores fatales.
- APK release SHA256 6939d8fd994764a3be0f284e138177228f1f71d7bae2d2108c0fb16f505b6450.
- APK Fire SHA256 cca341984653aa56af3c4ae401176f78d558d1264c90d182f9fc14ef06eee699.
- 589 entradas de contenido iguales, firma de prueba existente verificada.
- Evidencia /srv/drbep-dev/artifacts/catchup-genres-771/.
- Beta 771 instalada, sin publicar, commit/push ni modificaciones de producción.
