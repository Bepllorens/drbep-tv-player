# Beta 786 universal: preparación — 03/11/2026

## Estado

Candidata compilada y firmada en 192.168.93.185. No instalada ni publicada. El usuario ha indicado durante esta operación que está fuera de casa: solicita dejar la prueba del Fire pendiente y no puede consultar ahora la Xiaomi. La publicación queda después de las comprobaciones físicas solicitadas.

No se ha encendido el Fire, detenido la app, instalado APK, borrado datos ni modificado beta/stable/rescue. No se ha desplegado ni reiniciado el backend.

## Candidata congelada

- Versión: 786 / 2.0.786-beta-imported-catchup.
- Paquete: com.drbep.tvplayer.offline.
- Canal compilado: beta.
- Arquitecturas verificadas dentro del APK: armeabi-v7a y arm64-v8a.
- No es debuggable.
- Tamaño: 104.854.670 bytes.
- Firma de distribución válida, certificado SHA256: 0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b.
- APK SHA256: 770da1ae69d2b9f0c70ea5b8e7684674f83ad7c03a009ac74a608b0491e56df7.
- Fuente base: c7870d5fce5b08785c7188214d30fd28822834f0, rama qoder/disneyplus-vod-582.
- Única diferencia de código/configuración respecto a esa base: incremento de versión en app/build.gradle. No se cambia la selección de motores ni las rutas de proveedores.

APK para validar y posteriormente publicar, sin recompilar:
 /srv/drbep-dev/reviews/beta786-universal-20261103/drbep-786-universal.apk

Compilación mediante /srv/drbep-dev/build-signed-beta.py --universal, que carga la firma en memoria y no publica. BUILD SUCCESSFUL, 3 min 5 s. Verificados certificado, paquete, versión real del APK, ABIs y UPDATE_CHANNEL de la compilación.

Seis pruebas ImportedChannelCatchupTest correctas con configuración beta/universal. La revisión de fuente c7870d5 ya tenía 1.124 pruebas correctas; la ejecución de esta operación es la focalizada de seis, no una nueva regresión completa de 1.124.

## Respaldos y procedencia

Directorio privado del .185:
 /srv/drbep-dev/reviews/beta786-universal-20261103/

- beta785-before.apk: descargada del canal beta y verificada, SHA256 d043a15f0b1afc56dde88e4c471f1559d37456e7d62544a0a514e2870fb61723; firma de distribución válida.
- channels-before.json: metadatos anteriores de los tres canales.
- build.gradle.before y previous-build-output.apk: estado previo a esta preparación.
- candidate.json, badging.txt y signature-check.txt: identidad y comprobaciones.
- source-hashes.json: huellas de 905 archivos de fuente/recursos versionados, excluyendo documentación Markdown.
- build-universal.log, tests-catchup.log y tests-catchup.json.
- publication-request.pending.json: solicitud sin secretos preparada para el momento de publicar. Su apk_path es un destino futuro; la APK aún no se ha enviado al .223.
- release-notes.txt y SHA256SUMS.

No incorporar APK, bases, sesiones, credenciales ni estos archivos privados a Git. La versión y este documento se incluyen en la consolidación Git solicitada posteriormente por el usuario; la instalación y la publicación siguen pendientes. La copia de la candidata es independiente de app/build/outputs: una compilación posterior no debe sustituir silenciosamente el artefacto probado.

Canales públicos comprobados al terminar, con todos sus metadatos idénticos a la línea base:
- beta: 785.
- stable: 582.
- rescue: 543.

## Dispositivos y conservación de datos

El ADB del .185 detectó inicialmente el Fire 192.168.93.190:5555; al consultar su APK pasó a offline. No se ha podido realizar un respaldo nuevo del APK instalado ni leer su firma actual. No se da por probado el dispositivo con la televisión apagada.

La última APK privada conocida del Fire, final-fire.apk del ensayo del 02/11, tiene certificado 9f76a326703c87666c247a743679b5bf696aacf5f34d835bea53d369fcdfd200, distinto del certificado de distribución. Es una referencia histórica verificada en archivo, no una lectura del dispositivo actual.

Antes de actualizar el Fire hay que comprobar la firma realmente instalada. Si sigue siendo la de pruebas, Android no admite reemplazarla por el APK de distribución mediante una actualización normal. No desinstalar, borrar datos, debilitar verificaciones ni cambiar el linaje de firma para forzarlo. Mantener la instalación y sus datos mientras se decide una vía compatible. Un APK de prueba con payload equivalente y firma anterior permite una regresión funcional, pero no acredita instalar el APK de distribución; distinguir ambos resultados.

La Xiaomi no aparece en ADB ni en descubrimiento inalámbrico. Su IP/puerto actuales no están disponibles. Usar /srv/drbep-dev/tools/adb-modern -P 5038 para ella, conservando el servidor 5037 que controla el Fire. No guardar códigos de emparejamiento.

## Secuencia al volver el usuario

1. Confirmar TV encendida en la entrada HDMI del Fire y mando libre. Conectar la Xiaomi con sus IP/puerto actuales.
2. Inventariar versiones, arquitecturas y firmas de las dos instalaciones. Guardar APK y respaldos de datos mediante los mecanismos autorizados disponibles. Comprobar que los respaldos son utilizables; una copia de APK por sí sola no conserva la base privada ni Android Keystore.
3. Actualizar en el sitio cuando la firma sea compatible, sin desinstalar ni borrar datos. Comparar cuenta/activación, catálogos importados, favoritos y progreso antes/después.
4. Probar el catchup de La 1 HD Vodafone configurada: inicio real del archivo, pausa, avance conservando pausa, reanudación, regreso al directo y miniatura. Incluir regresión del catchup existente de La 1 Movistar ISM y navegación/foco. No extrapolar a todos los canales.
5. En Xiaomi, comprobar también las acciones táctiles de avance y retorno. Distinguir instalación por ADB de actualización OTA por el actualizador de la app.
6. Con las comprobaciones requeridas completadas, verificar que la versión786 no ha sido ocupada, SHA/firma de la candidata y estado actual del canal; guardar respaldo nuevo si hubo cambios.
7. Enviar el APK validado al destino previsto, ejecutar preflight y publicar únicamente beta. No promover stable, modificar rescue ni reiniciar el backend.
8. Respetar el bloqueo por sesiones activas. El script antiguo scripts/publish_offline_update.sh incluye X-Ops-Override:1; no usar esa excepción sin autorización específica para esta publicación. Preparar la petición por el mecanismo actual sin excepción automática.
9. Descargar el APK público de beta y cotejar SHA, certificado, versión y ABIs con esta candidata. Comparar stable/rescue con su estado anterior. Dejar constancia separada de instalación, reproducción, publicación y Git.

La autorización del usuario para publicar beta se conserva; lo pendiente ahora es su condición de validar ambos dispositivos y cualquier excepción operativa específica que el servidor pudiera exigir.

## Consolidación Git del 03/11/2026

Se guarda la versión786 y esta documentación en qoder/disneyplus-vod-582 por petición expresa del usuario. Se cotejó de nuevo el SHA256 de la candidata congelada y el informe de seis pruebas focalizadas sin fallos. No se recompiló, instaló ni publicó durante el commit/push y no se cambió beta/stable/rescue.
