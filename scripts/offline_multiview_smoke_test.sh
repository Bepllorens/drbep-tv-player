#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-192.168.93.189:5555}"
APP_PACKAGE="${APP_PACKAGE:-com.drbep.tvplayer.offline}"
APP_ACTIVITY="${APP_ACTIVITY:-com.drbep.tvplayer.MainActivity}"
WAIT_BOOT_SECONDS="${WAIT_BOOT_SECONDS:-12}"
WAIT_MULTIVIEW_SECONDS="${WAIT_MULTIVIEW_SECONDS:-15}"
EXPECTED_MIN_WINDOWS="${EXPECTED_MIN_WINDOWS:-2}"
PLAYBACK_CHANNEL_ID="${PLAYBACK_CHANNEL_ID:-1103938}"
SCREENSHOT_OUTPUT="${SCREENSHOT_OUTPUT:-}"
LOG_OUTPUT="${LOG_OUTPUT:-}"

usage() {
  cat <<EOF
Uso: $0 [DEVICE]

Comprueba en un Fire TV real que Multipantalla se abre desde Herramientas,
renderiza al menos dos canales y que el flujo principal vuelve a reproducir
despues de cerrarla.

Variables opcionales:
  APP_PACKAGE               Paquete Android. Default: $APP_PACKAGE
  APP_ACTIVITY              Activity principal. Default: $APP_ACTIVITY
  WAIT_BOOT_SECONDS         Espera de arranque. Default: $WAIT_BOOT_SECONDS
  WAIT_MULTIVIEW_SECONDS    Espera de las ventanas. Default: $WAIT_MULTIVIEW_SECONDS
  EXPECTED_MIN_WINDOWS      Minimo de ventanas/primeros frames. Default: $EXPECTED_MIN_WINDOWS
  PLAYBACK_CHANNEL_ID       Canal estable para arranque y recuperacion. Default: $PLAYBACK_CHANNEL_ID
  SCREENSHOT_OUTPUT         PNG opcional de la multipantalla.
  LOG_OUTPUT                Logcat opcional de la multipantalla.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

ADB=(adb -s "$DEVICE")
UI_XML="$(mktemp "${TMPDIR:-/tmp}/drbep-multiview-ui.XXXXXX.xml")"
MULTIVIEW_LOG="$(mktemp "${TMPDIR:-/tmp}/drbep-multiview-log.XXXXXX.log")"
RECOVERY_LOG="$(mktemp "${TMPDIR:-/tmp}/drbep-multiview-recovery.XXXXXX.log")"

cleanup() {
  rm -f "$UI_XML" "$MULTIVIEW_LOG" "$RECOVERY_LOG"
}
trap cleanup EXIT

if [[ "$DEVICE" == *:* ]]; then
  adb connect "$DEVICE" >/dev/null || true
fi
if ! "${ADB[@]}" get-state >/dev/null 2>&1; then
  echo "No se puede acceder al dispositivo ADB: $DEVICE" >&2
  exit 1
fi
if ! "${ADB[@]}" shell pm path "$APP_PACKAGE" >/dev/null 2>&1; then
  echo "No esta instalado el paquete $APP_PACKAGE en $DEVICE" >&2
  exit 1
fi
if [ -n "$PLAYBACK_CHANNEL_ID" ] && [[ "$PLAYBACK_CHANNEL_ID" == *[!0-9]* ]]; then
  echo "PLAYBACK_CHANNEL_ID debe ser un ID numerico de canal" >&2
  exit 1
fi

dump_ui() {
  "${ADB[@]}" shell uiautomator dump /sdcard/drbep-multiview-smoke.xml >/dev/null
  "${ADB[@]}" shell cat /sdcard/drbep-multiview-smoke.xml > "$UI_XML"
}

focused_label() {
  python3 - "$UI_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    if node.attrib.get("focused") != "true":
        continue
    parts = []
    for descendant in node.iter("node"):
        for key in ("content-desc", "text"):
            value = descendant.attrib.get(key, "").strip()
            if value and value not in parts:
                parts.append(value)
    print(" | ".join(parts))
    break
PY
}

ui_text() {
  python3 - "$UI_XML" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
parts = []
for node in root.iter("node"):
    for key in ("content-desc", "text"):
        value = node.attrib.get(key, "").strip()
        if value and value not in parts:
            parts.append(value)
print("\n".join(parts))
PY
}

press() {
  "${ADB[@]}" shell input keyevent "$1" >/dev/null
  sleep 1
}

focus_until() {
  local expected="$1"
  local keycode="$2"
  local attempts="$3"
  local label=""
  local attempt
  for ((attempt = 0; attempt <= attempts; attempt++)); do
    dump_ui
    label="$(focused_label)"
    if [[ "$label" == *"$expected"* ]]; then
      echo "Foco: $label"
      return 0
    fi
    press "$keycode"
  done
  echo "No se encontro el foco '$expected'. Ultimo foco: $label" >&2
  return 1
}

echo "== DRBEP multiview smoke test =="
echo "Dispositivo: $DEVICE"
"${ADB[@]}" shell dumpsys package "$APP_PACKAGE" 2>/dev/null \
  | grep -E "versionName=|versionCode=" \
  | head -n 2 \
  | sed 's/^[[:space:]]*/  /'

"${ADB[@]}" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
sleep 1
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" \
  --es reminder_channel_id "$PLAYBACK_CHANNEL_ID" \
  --es reminder_action play >/dev/null
sleep "$WAIT_BOOT_SECONDS"

if [ -z "$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)" ]; then
  echo "La app no sobrevivio al arranque" >&2
  exit 1
fi

dump_ui
if ui_text | grep -q "¿Qué quieres ver?"; then
  echo "Portada activa: entrando en Directo"
  press KEYCODE_DPAD_CENTER
  sleep 3
fi

press KEYCODE_MENU
dump_ui
if ! ui_text | grep -q "Herramientas"; then
  echo "No se abrio Herramientas sobre el canal estable" >&2
  exit 1
fi

focus_until "Opciones avanzadas" KEYCODE_DPAD_DOWN 10
press KEYCODE_DPAD_CENTER
dump_ui
if ! ui_text | grep -q "Opciones avanzadas"; then
  echo "No se abrio Opciones avanzadas" >&2
  exit 1
fi

focus_until "Ver varios canales" KEYCODE_DPAD_DOWN 6
press KEYCODE_DPAD_CENTER
dump_ui
if ! ui_text | grep -q "Mosaico 2x2"; then
  echo "No se abrio el menu Ver varios canales" >&2
  exit 1
fi
focus_until "Mosaico 2x2" KEYCODE_DPAD_DOWN 3
"${ADB[@]}" logcat -c || true
press KEYCODE_DPAD_CENTER

# La cabecera y la rejilla se ocultan al dejar la reproduccion sin controles.
# La entrada se valida por el recorrido de Herramientas y la capacidad real
# mediante primeros frames y READY de canales distintos, aunque la cabecera ya
# se haya autoocultado cuando uiautomator termina el volcado.
dump_ui
TITLE="$(ui_text | grep -m1 '^Multiview · ' || true)"
WINDOWS="$EXPECTED_MIN_WINDOWS"
if [ -z "$TITLE" ]; then
  TITLE="Multiview · capacidad confirmada por reproduccion"
fi
if [ -n "$SCREENSHOT_OUTPUT" ]; then
  "${ADB[@]}" exec-out screencap -p > "$SCREENSHOT_OUTPUT"
  echo "Captura guardada en: $SCREENSHOT_OUTPUT"
fi
if [ "$WAIT_MULTIVIEW_SECONDS" -gt 1 ]; then
  sleep "$((WAIT_MULTIVIEW_SECONDS - 1))"
fi

PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "La app se cerro al abrir Multipantalla" >&2
  exit 1
fi

"${ADB[@]}" logcat -d --pid="$PID" -v threadtime > "$MULTIVIEW_LOG" || true
if grep -qiE "FATAL EXCEPTION|AndroidRuntime.*FATAL|IllegalStateException|PlaybackException|ExoPlaybackException" "$MULTIVIEW_LOG"; then
  echo "Se detectaron errores graves al abrir Multipantalla:" >&2
  grep -iE "FATAL EXCEPTION|AndroidRuntime.*FATAL|IllegalStateException|PlaybackException|ExoPlaybackException" "$MULTIVIEW_LOG" | tail -n 20 >&2
  exit 1
fi

FIRST_FRAME_CHANNELS="$(sed -n 's/.*firstFrame channel={[[:space:]]*\([^,}]*\).*/\1/p' "$MULTIVIEW_LOG" | sort -u | grep -c . || true)"
READY_CHANNELS="$(sed -n 's/.*playbackBufferingEnd channel={[[:space:]]*\([^,}]*\).*state=READY.*/\1/p' "$MULTIVIEW_LOG" | sort -u | grep -c . || true)"
if [ "$FIRST_FRAME_CHANNELS" -lt "$EXPECTED_MIN_WINDOWS" ] || [ "$READY_CHANNELS" -lt "$EXPECTED_MIN_WINDOWS" ]; then
  echo "Multipantalla incompleta: firstFrame=$FIRST_FRAME_CHANNELS ready=$READY_CHANNELS esperado=$EXPECTED_MIN_WINDOWS" >&2
  exit 1
fi
WINDOWS="$FIRST_FRAME_CHANNELS"

if [ -n "$LOG_OUTPUT" ]; then
  cp "$MULTIVIEW_LOG" "$LOG_OUTPUT"
  echo "Log guardado en: $LOG_OUTPUT"
fi

echo "Multipantalla: $TITLE"
echo "Canales con primer frame: $FIRST_FRAME_CHANNELS"
echo "Canales READY: $READY_CHANNELS"

# Tras ocultarse la rejilla, el primer Atrás vuelve de la ventana ampliada a la
# rejilla y el segundo cierra Multipantalla. Si la rejilla seguia visible, el
# segundo Atrás solo actua sobre el launcher. La reapertura debe restaurar el
# canal principal y producir un frame nuevo.
press KEYCODE_BACK
press KEYCODE_BACK
"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" \
  --es reminder_channel_id "$PLAYBACK_CHANNEL_ID" \
  --es reminder_action play >/dev/null
sleep "$WAIT_BOOT_SECONDS"
RECOVERY_PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$RECOVERY_PID" ]; then
  echo "La app no se recupero despues de cerrar Multipantalla" >&2
  exit 1
fi
"${ADB[@]}" logcat -d --pid="$RECOVERY_PID" -v threadtime > "$RECOVERY_LOG" || true
if grep -qiE "FATAL EXCEPTION|AndroidRuntime.*FATAL|PlaybackException|ExoPlaybackException" "$RECOVERY_LOG"; then
  echo "Se detecto un error al recuperar el flujo principal" >&2
  exit 1
fi
if ! grep -q "firstFrame channel=" "$RECOVERY_LOG"; then
  echo "El flujo principal no produjo un primer frame tras cerrar Multipantalla" >&2
  exit 1
fi

echo "Recuperacion principal: OK"
echo "Multiview smoke OK: $WINDOWS ventanas reales, sin colisiones de sesion y con recuperacion."
