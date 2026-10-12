# Beta 722: TV, guía y miniatura de Inicio

Canal beta publicado el 12 de octubre de 2026. Incluye los cambios acumulados del panel del dispositivo, guía y presentación de programas, además de las optimizaciones de navegación de TV y guía.

Al regresar desde pantalla completa, la miniatura del canal actual se adjunta sin la espera inicial de selección. Los cambios posteriores de foco conservan el debounce. La miniatura utiliza TextureView para vídeo ordinario y conserva SurfaceView para Widevine/PlayReady; el nuevo PlayerView se mide explícitamente al insertarlo en AndroidView. El diagnóstico ahora registra su visibilidad y tamaño reales.

Validación: release compilada; 664 pruebas JVM, sin fallos. Tres retornos de pantalla completa a Inicio en M+ Originales en el Fire .190 recuperaron vídeo. Los dos últimos primeros fotogramas llegaron en 1057 y 1069 ms desde el cambio de superficie. No se afirma una transición instantánea ni validación de todos los proveedores.

La APK oficial se firmó desde la APK release de prueba sin recompilar: se verificaron idénticos los 853 archivos de contenido, excluyendo firmas. La firma de pruebas del Fire y la oficial son diferentes; el Fire conserva la primera. Ambas APK incluyen armeabi-v7a y arm64-v8a.

- Versión: 2.0.722-beta-preview-surface (722).
- APK oficial SHA-256: 3a797ed108243a4501f942f008b13c0499af3bbb00d3b87b40de459c9b1fda93.
- Certificado oficial SHA-256: 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b.
- Descarga: https://fire.tvbep.com/api/offline/app/apk?channel=beta
- Respaldo del canal: /opt/drbep-releases/beta722-publication-20261012 en el servidor .223.
- Stable 582 y rescue 543: metadatos y hashes sin cambios. Sin reinicio del backend.

Las pruebas intermedias de superficie/tamaño por separado no bastaban; se descartaron como solución completa. Los artefactos locales y las capturas están en artifacts/preview-722 del workspace DRBEP, fuera de este repositorio.
