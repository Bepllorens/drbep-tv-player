# DRBEP TV Player (Android TV)

Reproductor nativo para Android TV, Fire TV y tablet. Usa Media3/ExoPlayer y una
ruta libVLC acotada para formatos que Media3 no puede reproducir correctamente.

La edición offline consume un catálogo firmado de DRBEP, reproduce en direct play
y mantiene permisos, telemetría y dispositivos asociados al usuario.

## Fire Stick controls

- `DPAD UP/DOWN`: zapping canal anterior/siguiente.
- `DPAD LEFT/RIGHT`: abrir lista de canales.
- `OK/ENTER`: confirma canal en la lista, o play/pause sin lista.
- `BACK`: cierra lista (o sale de la app).
- `MENU`: mostrar/ocultar lista de canales.
- `MENU` x2 rapido: activar/desactivar modo "solo favoritos".
- `CHANNEL +/-` y `PAGE +/-`: zapping rapido.
- `PLAY/PAUSE`: pausar/reanudar.
- `OK` mantenido (long press) sobre un canal en la lista: añadir/quitar favorito.
- `GUIDE`: abrir la guía temporal.
- `SEARCH`: abrir búsqueda global.

La app tambien:

- ofrece accesos principales a Directo, Guía, Grabaciones y Biblioteca,
- agrupa películas, series y episodios de Plex,
- incluye EPG, U7D, grabaciones, multipantalla, favoritos y listas,
- mantiene modo inmersivo fullscreen,
- recupera foco al volver a primer plano,
- recuerda el ultimo canal reproducido,
- sincroniza favoritos, recientes, progreso VOD y presets con la web app,
- permite búsqueda por voz y PiP cuando el dispositivo los soporta.

## Build

```bash
cd /opt/drbep-tv-player
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew assembleDebug -PplayerUrl="http://TU_IP:8080/player.html"
```

Release segura por defecto:

```bash
cd /opt/drbep-tv-player
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew assembleRelease -PreleasePlayerUrl="https://tu-dominio/player.html"
```

Notas:

- `debug` permite `cleartext` para pruebas locales.
- `release` fuerza una configuracion de red mas cerrada y no usa una IP LAN hardcodeada por defecto.
- Si quieres fijar una URL especifica para Fire Stick en `debug`, usa `-PdebugForceFirestickUrl=true -PdebugFirestickLockedUrl="http://IP:8080/player.html"`.

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release output:

```text
app/build/outputs/apk/release/app-release.apk
```

## Install on TV (ADB)

```bash
scripts/install_tv.sh IP_DE_TV:5555
```

El script instala `com.drbep.tvplayer.offline`, arranca `MainActivity` y desactiva paquetes antiguos conocidos
como `com.drbep.tv.v2.fixed` para evitar abrir una app vieja desde el launcher.

Para auditar que el Fire Stick esta usando el paquete correcto:

```bash
scripts/check_tv_install.sh IP_DE_TV:5555
```
