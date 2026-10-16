# Beta 747: caja negra EPG, catchup y multipantalla
Fecha: 2026-10-16. Desarrollo en .185; base Git 6ed9628 con cambios anteriores conservados.
No se ha publicado, realizado commit/push ni modificado el backend.

## Validación técnica
708 tests, cero fallos/errores/omitidos. Release universal compilada.
JavaScript del panel: node --check correcto. git diff --check correcto.
Firma oficial y APK compatible con firma preexistente del Fire preparadas por separado.
SHA256 release: 352239f09e8d1055aa1f779babe68370b637c28eaee686dab7cd5b4c8c654323
SHA256 Fire: fec1ec526f21763a39091bfb0de3fbfed2d9f601b06652c2d01288ec2cb8f10d
No publicar la APK de prueba Fire como release oficial.

## Fire .190
Instalación -r correcta, beta 747 visible.
Observado: arranque, portada, catchup con carga progresiva y página de 60,
directo La 1, dos ventanas La 1/La 2 con vídeo, salida de multipantalla.
Panel local emparejado temporalmente y ZIP exportado; cierre al finalizar.
348 eventos propios de 747 en exportación inicial; esquema de nueve campos
verificado, números salvo nombre fijo del evento y UUID de sesión.
Sin eventos perdidos, errores de escritura, PLAY_ERROR, CRASH ni UI_STALL
durante ese recorrido. Esto no equivale a una prueba exhaustiva de estabilidad.

Dos ventanas diferenciadas por op: primeras imágenes 1578 y 2604 ms.
Cuatro muestras de contadores, cero frames descartados en esas muestras.
Aperturas y cierres registrados para ambas. Sin nuevo temporizador por ventana.
31 respuestas de guía HTTP 200. Se observan eventos de caché, parseo y carga.

## Hallazgo para investigar después
CATCHUP_PAGE: mediana 160 ms, P95 647 ms, máximo 20843 ms
(60 filas, consulta completada). CATCHUP_STRIP: mediana 145 ms,
P95 2025 ms, máximo 4621 ms. EPG_LOAD: mediana 209 ms, máximo 923 ms.
Son tiempos de esta sesión, no un benchmark comparativo.
El máximo de 20,8 s incluye la operación SQLite completa; no demuestra
por sí solo si domina espera por bloqueo, consulta, deserialización o carga
simultánea. Añadir submedidas al investigar antes de cambiar la base.
No se han optimizado ni alterado consultas/rutas en esta ampliación.

Artefactos privados en /srv/drbep-dev/artifacts/beta747-blackbox:
tests.log, build.log, release.apk, fire.apk, fire-validation.zip.
No incluye PIN ni token del panel. El registro no guarda canales,
títulos, URLs, claves ni credenciales.
