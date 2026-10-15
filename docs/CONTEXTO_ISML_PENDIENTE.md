# Estado de la compatibilidad ISML y pruebas pendientes

Fecha: 15 de octubre de 2026. Documento sin credenciales ni material de descifrado.

## Objetivo y alcance

El usuario quiere ampliar la compatibilidad de catchup con ISML/SmoothStreaming y comprobarla en el Fire .190, manteniendo intactas las rutas que ya funcionan, especialmente La 1 y Vamos.

Este documento recoge arquitectura, estado y validacion. No contiene instrucciones para sustituir el sistema de licencias del proveedor ni reutilizar claves fuera de ese sistema. La integracion descrita debe utilizar contenido sin cifrar o una licencia oficial compatible con el dispositivo.

## Entorno de trabajo

Desarrollar exclusivamente en 192.168.93.185 como drbep-dev, mediante ssh drbep-dev-185. Cargar /srv/drbep-dev/env.sh. El Mac se utiliza para ADB y capturas, no para compilar.

Leer primero los AGENTS.md y estos documentos:
- /srv/drbep-dev/repos/drbep/docs/CONTEXTO_DRBEP_PARA_OTRA_IA.md
- /srv/drbep-dev/repos/drbep/docs/INICIO_OTRA_IA.txt

Backend: /srv/drbep-dev/repos/drbep
Rama: codex/backend-migration-20261015
HEAD comprobado: 8d91951cadf54457f748d1abd419271dc2ddc9cd

Android: /srv/drbep-dev/repos/drbep-tv-player
Rama: qoder/disneyplus-vod-582
HEAD comprobado: e4f5d93067beac703aec163ecb77948972d08fca

Los cambios Android de la beta 737 se incorporan junto con este documento en un commit posterior al HEAD de referencia indicado arriba. Consultar git log para identificarlo y revisar git status antes de continuar. La APK 737 no se ha publicado en el canal beta.

Produccion esta en .223. Esta entrega no autoriza desplegar, reiniciar, publicar APK ni modificar beta/stable.

## Hallazgos comprobados en el codigo

El cliente ya reconoce SmoothStreaming y dispone de rutas que asignan MimeTypes.APPLICATION_SS. Por tanto, no falta todo el soporte del formato en Android.

La ruta nativa de catchup inspeccionada en el backend construye y valida manifiestos DASH/MPD. No es una ruta generica de manifiestos: admitir ISML requiere revisar el contrato de formato entre backend y cliente, no limitarse a cambiar la extension de una URL.

No se ha implementado ni probado una nueva ruta ISML de catchup. Tampoco se ha demostrado que un cambio de formato, por si solo, resuelva las diferencias de compatibilidad del proveedor.

## Archivos Android relevantes

Base absoluta: /srv/drbep-dev/repos/drbep-tv-player/

- app/src/main/java/com/drbep/tvplayer/PlaybackRouteResolver.java: decision de ruta y MIME; reconocimiento de SmoothStreaming.
- app/src/main/java/com/drbep/tvplayer/PlayerController.java: creacion y gestion del reproductor; tratamiento del formato SmoothStreaming y decisiones de motor.
- app/src/main/java/com/drbep/tvplayer/VlcFallbackPolicy.java: restricciones del motor alternativo para contenido protegido.
- app/src/main/java/com/drbep/tvplayer/MainActivity.java: coordinacion de la reproduccion y la interfaz.
- app/src/test/java/com/drbep/tvplayer/PlaybackRouteResolverTest.java
- app/src/test/java/com/drbep/tvplayer/VlcFallbackPolicyTest.java
- app/src/test/java/com/drbep/tvplayer/PlayerControllerCompatibilityFallbackTest.java
- docs/beta737-validation.txt: resultados y limites de las pruebas anteriores.

## Archivos backend relevantes

Base absoluta: /srv/drbep-dev/repos/drbep/

- internal/api/movistar_u7d_native.go: ruta nativa de archivo y validacion de manifiestos; actualmente orientada a DASH.
- internal/api/movistar_u7d_native_test.go: pruebas unitarias asociadas.
- internal/api/movistar_u7d_native_live_test.go: pruebas de integracion; no ejecutarlas sin revisar sus efectos y requisitos.
- internal/api/movistar_archive_resolver.go: resolucion de archivo.
- internal/api/movistar_archive_resolver_test.go: pruebas del resolver.

Las rutas se proporcionan para situar la arquitectura; no incluyen endpoints privados, sesiones, claves ni datos de reproduccion.

## Cambios realizados y no realizados

En las dos revisiones dedicadas a ISML solo se leyo codigo. No se modifico ni revirtio codigo de ISML, no se desplego backend y no se alteraron La 1 o Vamos.

La beta 737 anterior incorpora mejoras de navegacion y busqueda. Tambien refuerza la comprobacion de elegibilidad antes de entrar al motor VLC. Ese cambio no constituye una implementacion ISML.

## Validacion existente y pendiente

Beta 737: 694 pruebas unitarias correctas y compilacion universal correcta. Instalada en el Fire conservando datos mediante una copia compatible con la firma que tenia el dispositivo. La APK oficial y la copia para ese Fire son artefactos distintos; no publicar la copia de prueba como release oficial.

Se comprobaron navegacion, busqueda, categorias y retorno desde menus al video. Estas pruebas NO validan catchup ISML. No declarar reproduccion funcional basandose solo en HTTP 200, un manifiesto parseable o una compilacion correcta.

Para una integracion compatible con licencias oficiales:
1. Confirmar que el proveedor ofrece un manifiesto ISML y licencia compatibles con MediaDrm y el Fire objetivo, sin extraer ni trasladar claves.
2. Probar primero el formato con contenido de prueba autorizado, manteniendo las rutas actuales por defecto.
3. Verificar deteccion MIME, errores de formato y seleccion del motor con pruebas aisladas.
4. Con la TV encendida y el mando libre, comprobar inicio de reproduccion, avance, timeshift, retorno al directo y vuelta a miniatura.
5. Incluir La 1 y Vamos como referencias de regresion sin modificar sus asociaciones, URLs ni configuracion para esta prueba.

No se han recogido nuevos registros de fallo ISML del Fire porque no se ha ejecutado una nueva ruta ISML.
