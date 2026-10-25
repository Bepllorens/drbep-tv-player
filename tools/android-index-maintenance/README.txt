Auxiliar temporal de adopcion de indice, solo para un dispositivo autorizado.
No es parte de la APK distribuida. No contiene claves ni datos de usuario.
Debe firmarse con la MISMA identidad del paquete ya instalado; nunca generar
otra ni desinstalar DRBEP. No publicarlo ni dejarlo instalado tras mantenimiento.
No incluir Kotlin runtime (duplica clases del APK minificado y puede abortar ART).

Compilar en .185 con env.sh y ../../gradlew assembleDebug desde esta carpeta.
Acciones explicitamente invocadas via am instrument: inspect, backup, export,
clonecheck, bench, apply, rollback. Destino fijo com.drbep.tvplayer.offline.
Backup solo si version8 e indice ausente y sin tareas activas. Preserva rowids,
congelando escritor y WAL; integridad y huellas de datos/esquema. Exportacion
loopback autenticada, un archivo fijo y un uso45s; no publicar PIN/token.
Antes de aplicar, descargar respaldo en privado a185 y verificar SHA256.
clonecheck crea/retira indice en copia real y prueba helper8; bench solo en copia.
apply conserva version8 y exige datos iguales al respaldo verificado.
rollback quita SOLO indice esperado, preserva datos actuales y version8; no
restaurar una copia vieja sobre progreso nuevo. No usar contra otra version o
indice con columnas diferentes. Guardar APK instalada como retorno adicional.

Helpers MetadataIndexQueue/Selection se copian del checkout cambiando solo el
package para evitar colisiones con el APK ofuscado; mantenerlos alineados al
reutilizar la prueba. Tras terminar retirar auxiliar y restaurar filtro dexopt
si se cambio durante diagnostico. No ejecutar nada automaticamente al compilar.

25/10 candidata conjunta: snapshot opcional (YYYYMMDD o YYYYMMDD-sufijo),
permite un respaldo8 que ya contenga el indice verificado. No sobreescribirlo.
releasecheck prueba en COPIA8 ->9 ->8 con integridad/digest y apertura helper8.
return8 solo para vivo9 y respaldo8 con esquema identico salvo indice propio;
valida columnas y conserva todos los datos ACTUALES, incluso progreso posterior.
No restaura filas del respaldo. Rechaza diferencias de esquema ajenas al indice.
Ejecutar detenido, sin indexador/importaciones activas; despues instalar APK8
compatible conservando datos. Si difiere el esquema, abortar y revisar manualmente.
No extrapolar este retorno a otras migraciones ni ejecutar automaticamente.

Auxiliar v2: targetSdk34 como cliente, preferir assembleRelease (sin debuggable).
En Xiaomi/Android16 se necesita confirmar el aviso Instalar viaUSB dentro de10s.
Una denegacion de ese aviso NO demuestra bloqueo de firma, ABI o targetSdk.
No desactivar verificaciones ni asumir que cambiar target elimino el rechazo:
el caso observado se resolvio confirmando Instalar, sin recordar la eleccion.
