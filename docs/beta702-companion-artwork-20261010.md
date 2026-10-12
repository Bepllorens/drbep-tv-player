# Beta 702: imágenes y panel del dispositivo

Publicada el 10 de octubre de 2026 con autorización expresa para publicar con sesiones activas. No se reinició el servidor. Stable continúa en 582 y rescue en 543; sus metadatos y hashes no cambiaron.

## Cambios
- Miniaturas horizontales de la guía oficial de Movistar, con coincidencia por canal, horario y título. Caché de índice en disco y memoria, precarga limitada y caché de imágenes de Glide. La guía fanart existente continúa como respaldo.
- Panel web: control de reproducción, selección de episodios por temporada, progreso, marcar vistos y filtros de catchup, fichas y envío al televisor.
- Imágenes web cargadas según visibilidad y con concurrencia limitada. Mantiene la autenticación del panel y no entrega URLs privadas de reproducción al navegador.
- Mensajes de importación más claros y migración de la base de datos v5 a v6 conservando favoritos y progreso.

## Validación
652 pruebas JVM sin fallos. Suite del panel web con Playwright aprobada. Cuatro pruebas instrumentadas en el Fire aprobadas: migración, selección y descarga de imagen horizontal real y acceso autenticado al panel del dispositivo.

El Fire conserva sus datos y tiene instalada la compilación de pruebas 702 con su firma debug compatible. La APK oficial de publicación se compiló en release con R8 y firma oficial; no es el mismo binario instalado en el Fire. No se ha validado la reproducción de todos los proveedores ni se declara resuelto el fallo previo de U7D de Movistar Plus.

## Publicación
Versión: 2.0.702-beta-companion-artwork. Canal: beta.
APK oficial universal: drbep-beta702-universal-release.apk.
SHA-256: bed18293a8918b09063872e03b0a6f6b40c8e42b192523e3dd2011ddea5baf37.
URL: https://fire.tvbep.com/api/offline/app/apk?channel=beta
Respaldo de publicación: /opt/drbep-releases/beta702-publication-20261010 en 192.168.93.223.

source.patch y source/ conservan el estado acumulado del checkout, incluyendo cambios anteriores de las betas 699-701. No se ha creado commit ni push en esta operación.
