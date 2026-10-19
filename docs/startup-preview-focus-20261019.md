# Foco de arranque y petición de catchup — beta 765

## Comportamiento
En TV, la primera miniatura de directo de Inicio pide foco al aparecer. La marca vive en Discovery, por instancia de Activity, y se consume al recibir foco. Cualquier pulsación previa cancela el enfoque tardío para no interrumpir la navegación. Al volver a Inicio se restaura la miniatura si fue el último objetivo de foco, mediante la misma memoria de selección de portada. Sin canal disponible se conserva el foco de navegación; en tablet no se fuerza este nuevo foco.

La miniatura conserva su acción de pantalla completa. No se modifican el reproductor, las asociaciones, los formatos ni la base de datos.

## Catchup
Al pulsar un día distinto (o abandonar Guardados mediante un día), se actualizan día, modo y offset y se solicita la página inmediatamente. Se conserva una huella de filtros enviados para que LaunchedEffect no duplique esa petición. Los cambios de canal/género/búsqueda conservan el mecanismo existente, incluido el debounce de texto. El refresco de la guía no se elimina.

## Archivos
- app/src/main/java/com/drbep/tvplayer/StartupHomeHubUiModel.java
- app/src/main/java/com/drbep/tvplayer/StartupHomeHubComposeBinder.kt
- app/src/main/java/com/drbep/tvplayer/U7dCatalogComposeBinder.kt
- app/src/main/java/com/drbep/tvplayer/MainActivity.java
- app/build.gradle

La iteración 763 demostró que adelantar la petición no bastaba: mediana total 761,9 ms, selección→solicitud 0,2 ms pero solicitud→preparación 295,3 ms. No se presenta ese cambio aislado como mejora de latencia total.

La 765 conserva la copia de guardados que publica la pantalla, reiniciada por cuenta, y consulta inmediatamente si esa copia está disponible. Si no está disponible mantiene la lectura original. Las acciones de guardado siguen releyendo la base y publicando sus resultados.

Validación final:
- 721 tests, 0 fallos/errores.
- 589 entradas idénticas fuera de META-INF entre release y copia firmada para Fire.
- Fire: versión 765 confirmada. Arranque con miniatura enfocada; OK a directo; Atrás restaura miniatura y vídeo; izquierda a Inicio del menú lateral.
- Sin cambios al reproductor ni bases de datos.
- Artefactos privados: /srv/drbep-dev/artifacts/start-focus-765/
- Release SHA256: 31969730832d9c2805b65abad8f5c1f157eee63be3cbaadaa9791f0e1e4369ac
- Fire SHA256: c831e77734c0732c1e7d4da95dd2358a340ec1efa7454a763b5b575a9fb2111f

No se ha probado físicamente un arranque sin canales ni el cambio de cuenta en esta iteración. Esas rutas conservan el fallback inicial y la invalidación explícita.
No publicar ni desplegar producción como parte de esta tarea.

## Latencia Fire (12 cambios por tanda)
| Medida | Beta 762 | Beta 765 |
|---|---:|---:|
| Selección→dibujo, mediana | 733,8 ms | 546,2 ms |
| Máximo observado | 1658,72 ms | 929,71 ms |
| Selección→solicitud, mediana | 299,5 ms | 0,2 ms |
| Solicitud→preparación, mediana | 17,6 ms | 0,5 ms |

Mejora de mediana aproximada del 26%. Medición con guía y cachés reales, no base congelada: una tanda no prueba ausencia de picos. El marcador final es el dibujo Compose de la página publicada, no la descarga completa de todas las imágenes. Persisten costes de consulta, modelos y dibujo.

Se comprobó el guardado temporal de La Lego película, su aparición en Guardados y la acción de quitarlo (botón vuelve a Guardar). No se inició su reproducción.
No se publica esta beta ni se hace commit/push en esta tarea. Se conservan todos los cambios pendientes anteriores.

Incidencia observada en la prueba de guardados: el primer intento de quitar el elemento cambió la ficha a Guardar, pero una reapertura posterior volvió a mostrarlo guardado. No se determinó si fue una acción pendiente, una interacción repetida o un refresco con snapshot antiguo; no atribuir causa sin nueva evidencia. Se repitió la retirada con la guía estabilizada y se confirmó 0 programas tanto al terminar como tras cambiar a Hoy y volver a Guardados. El elemento temporal queda retirado. Revisar esta carrera por separado antes de afirmar paridad de guardados durante refrescos simultáneos.

## Continuación 766: guardados durante el refresco

Se identificó una carrera de presentación: callbacks de guía y disponibilidad
transportaban una lista de guardados leída antes de una acción del usuario, y
podían sustituir la caché reciente o entregar tarjetas con una marca antigua.
Además, la acción de la tarjeta alternaba el estado real: repetir «Quitar» antes
de recibir el resultado podía volver a insertar el programa.

La 766 conserva instantáneas inmutables con revisión. Lectura y escritura del
estado guardado se coordinan; la revisión avanza después de confirmar la
transacción. La presentación descarta instantáneas y páginas anteriores a una
modificación. Las tarjetas solicitan explícitamente guardar o quitar, de forma
idempotente; la acción existente del panel web conserva su semántica de alternar.
No cambia el esquema de la base de datos ni las rutas de reproducción.

Cuatro pruebas nuevas cubren respuesta antigua tras quitar, copia independiente,
rechazo de estado sin versión y protección contra mutación. Pendiente completar
la comprobación funcional de esta compilación en el Fire.

### Resultado funcional 766
- 725 pruebas, cero fallos y errores. APK instalado: 2.0.766-beta-saved-refresh.
- Arranque y vuelta a Inicio con miniatura visible y enfocada.
- Guardado temporal de «Don Juan, mi querido fantasma», marca Guardado confirmada.
  Doble pulsación de Quitar; Guardados quedó vacío y siguió vacío tras Actualizar,
  salir/reabrir catchup y doce cambios de día. El dato temporal quedó eliminado.
- No se reprodujo la reaparición observada en 765. La carrera está cubierta por
  pruebas deterministas; la prueba manual no demuestra todas las intercalaciones.
- 12 cambios de día: mediana 558,8 ms, máximo 876,68 ms. 765: 546,2/929,71 ms.
  Mantiene el orden de magnitud; no se afirma mejora estadística entre ambas tandas.
  La guía seguía recibiendo emisiones y mostraba avisos parciales de actualización.
- Evidencia privada: /srv/drbep-dev/artifacts/saved-refresh-766/.
  Release SHA256 bec6ccadc4a26953244e12d773795361684a705d97050ebed0a4aa228ef0ab45.
  Fire SHA256 4eb0c005eaa56291b64ba9ff791859fccde63c6f5ff644769bc0bda5dc5ec66b.
  Los 589 archivos ajenos a META-INF coinciden entre ambos APK.
- Sin publicar canal beta, sin commit/push y sin modificar producción.
