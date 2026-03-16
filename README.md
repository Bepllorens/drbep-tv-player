# DRBEP TV Player (Android TV)

Native player (Media3/ExoPlayer) para Android TV / Fire Stick.

Carga canales desde `GET /api/channels` y reproduce `play_url` directamente.

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

La app tambien:

- muestra overlay con canal actual,
- mantiene modo inmersivo fullscreen,
- recupera foco al volver a primer plano.
- recuerda el ultimo canal reproducido,
- guarda favoritos y los muestra primero en la lista.

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
adb connect IP_DE_TV:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
