# DRBEP TV Player Handoff (2026-03-16)

## Estado actual

- Repo: `/opt/drbep-tv-player`
- Rama: `main`
- HEAD actual: `d739b3d3b9131703362b0f0820ecba29e63e41c1`
- `origin/main` alineado con `HEAD`
- Build actual en `app/build.gradle`:
  - `versionCode 4`
  - `versionName "1.0.3"`

## Hitos ya integrados

- `v1.2` cerrada funcionalmente con:
  - overlay principal reforzado
  - favoritos ordenables
  - búsqueda rápida por mando
  - panel de grabaciones mejorado
  - acciones de grabación limitadas a las realmente soportadas
  - pulido de navegación entre overlays
- arte Fire TV migrado a `PNG` reales en `mipmap` y `drawable-nodpi`
- banner Fire TV revisado varias veces; último ajuste en commit `d739b3d`
- base de `v1.3.1` ya iniciada con guía timeline

## Timeline v1.3.1 ya hecha

- acceso a timeline desde `INFO` con overlay abierto
- acceso a timeline desde herramientas
- ventana temporal de `3h`
- botón `Ahora`
- botón `+2h`
- filas de varios canales cercanos al seleccionado
- bloques de programa con ancho por duración
- foco visual y scroll horizontal básicos
- click en bloque reutiliza acciones de programa ya existentes

Puntos clave en código:

- `openTimelineGuideAroundSelection()` en `app/src/main/java/com/drbep/tvplayer/MainActivity.java`
- `showTimelineGuideDialog(...)` en `app/src/main/java/com/drbep/tvplayer/MainActivity.java`
- layout `app/src/main/res/layout/dialog_timeline_guide.xml`

## Últimos commits relevantes

- `d739b3d` Widen Fire TV banner artwork
- `75b822c` Redesign Fire TV launcher art
- `1474de1` Use PNG launcher assets for Fire TV
- `5d5618c` Improve timeline focus
- `1f7ba84` Add timeline guide base
- `8f6c424` Add v1.3 plan

## Situación pendiente más inmediata

### Fire TV artwork

- El usuario confirmó que el icono/banner ya cambian.
- El último feedback fue: "ahora se ve bien, aunque sigue sin rellenar todo el ancho de la tarjeta".
- Se hizo una última revisión del banner en `d739b3d` para apurarlo más al ancho.
- Siguiente paso natural aquí: esperar confirmación visual final del usuario antes de tocar otra vez el arte.

### Timeline v1.3.1

Siguiente slice recomendado:

1. mejorar escala temporal y legibilidad de horas
2. mejorar navegación vertical entre filas/canales
3. decidir si mini guía y timeline conviven o si `INFO` queda definitivamente para timeline

## Archivos importantes

- `app/src/main/java/com/drbep/tvplayer/MainActivity.java`
- `app/src/main/res/layout/dialog_timeline_guide.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `V1_3_PLAN.md`
- `V1_3_CHECKLIST.md`

## Comandos útiles

Build debug:

```bash
cd /opt/drbep-tv-player
export ANDROID_SDK_ROOT=/opt/android-sdk
./gradlew assembleDebug
```

Instalar en Fire Stick:

```bash
adb connect 192.168.93.16:5555
adb install -r /opt/drbep-tv-player/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.drbep.tvplayer/.MainActivity
```

Instalación limpia si el launcher cachea arte:

```bash
adb uninstall com.drbep.tvplayer || true
adb install /opt/drbep-tv-player/app/build/outputs/apk/debug/app-debug.apk
```

## Nota práctica para retomar

Si se continúa desde Codex, el mejor punto de reentrada no es revisar todo `v1.2`; es:

1. confirmar visualmente el estado del banner Fire TV de `1.0.3`
2. seguir con `v1.3.1` mejorando la timeline
3. después pasar a la vista de grabaciones programadas de `v1.3.2`