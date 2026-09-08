#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-192.168.93.189:5555}"
APP_PACKAGE="${APP_PACKAGE:-com.drbep.tvplayer.offline}"
APP_ACTIVITY="${APP_ACTIVITY:-com.drbep.tvplayer.MainActivity}"
WAIT_SECONDS="${WAIT_SECONDS:-1}"
WAIT_PLAYBACK_SECONDS="${WAIT_PLAYBACK_SECONDS:-6}"
PLAYBACK_CHANNEL_ID="${PLAYBACK_CHANNEL_ID:-1103938}"
WORK_DIR="$(mktemp -d)"
REMOTE_XML="/sdcard/drbep-navigation-focus.xml"

cleanup() {
  adb -s "$DEVICE" shell rm -f "$REMOTE_XML" >/dev/null 2>&1 || true
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

ADB=(adb -s "$DEVICE")

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

send_key() {
  local keycode="$1"
  "${ADB[@]}" shell input keyevent "$keycode" >/dev/null
  sleep "$WAIT_SECONDS"
}

dump_ui() {
  local name="$1"
  local local_path="$WORK_DIR/$name.xml"
  "${ADB[@]}" shell uiautomator dump "$REMOTE_XML" >/dev/null
  "${ADB[@]}" pull "$REMOTE_XML" "$local_path" >/dev/null
  printf '%s\n' "$local_path"
}

ui_contains() {
  local xml_path="$1"
  local expected="$2"
  python3 - "$xml_path" "$expected" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
needle = sys.argv[2]
for node in root.iter():
    if needle in (node.attrib.get("text", ""), node.attrib.get("content-desc", "")):
        raise SystemExit(0)
raise SystemExit(1)
PY
}

focused_bounds() {
  local xml_path="$1"
  python3 - "$xml_path" <<'PY'
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
for node in root.iter():
    if node.attrib.get("focused") == "true":
        print(node.attrib.get("bounds", ""))
        raise SystemExit(0)
raise SystemExit(1)
PY
}

assert_visible() {
  local xml_path="$1"
  local expected="$2"
  if ! ui_contains "$xml_path" "$expected"; then
    echo "No aparece '$expected' en el estado de UI $xml_path" >&2
    exit 1
  fi
}

echo "== DRBEP navigation/focus smoke =="
echo "Dispositivo: $DEVICE"
echo "Canal estable: $PLAYBACK_CHANNEL_ID"

"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
"${ADB[@]}" shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" \
  --es reminder_channel_id "$PLAYBACK_CHANNEL_ID" \
  --es reminder_action play >/dev/null
sleep "$WAIT_PLAYBACK_SECONDS"

send_key KEYCODE_MENU
TOOLS_XML="$(dump_ui tools)"
assert_visible "$TOOLS_XML" "Herramientas"
assert_visible "$TOOLS_XML" "Directo"
assert_visible "$TOOLS_XML" "Guía"
assert_visible "$TOOLS_XML" "Grabaciones"
assert_visible "$TOOLS_XML" "Biblioteca"
TOOLS_FOCUS="$(focused_bounds "$TOOLS_XML")"
echo "Herramientas abiertas con foco: $TOOLS_FOCUS"

send_key KEYCODE_DPAD_DOWN
TOOLS_DOWN_XML="$(dump_ui tools-down)"
TOOLS_DOWN_FOCUS="$(focused_bounds "$TOOLS_DOWN_XML")"
if [ "$TOOLS_DOWN_FOCUS" = "$TOOLS_FOCUS" ]; then
  echo "El foco no avanzo al pulsar abajo en Herramientas" >&2
  exit 1
fi
echo "Foco desplazado en Herramientas: $TOOLS_DOWN_FOCUS"

send_key KEYCODE_DPAD_CENTER
EPG_XML="$(dump_ui epg-menu)"
assert_visible "$EPG_XML" "TV/EPG"
assert_visible "$EPG_XML" "Guia timeline"
EPG_FOCUS="$(focused_bounds "$EPG_XML")"
echo "Submenu TV/EPG abierto con foco: $EPG_FOCUS"

send_key KEYCODE_DPAD_CENTER
TIMELINE_XML="$(dump_ui timeline)"
assert_visible "$TIMELINE_XML" "Guia timeline"
assert_visible "$TIMELINE_XML" "Ahora"
assert_visible "$TIMELINE_XML" "Cerrar"
TIMELINE_FOCUS="$(focused_bounds "$TIMELINE_XML")"
echo "Timeline abierta con programa enfocado: $TIMELINE_FOCUS"

send_key KEYCODE_DPAD_RIGHT
TIMELINE_RIGHT_XML="$(dump_ui timeline-right)"
TIMELINE_RIGHT_FOCUS="$(focused_bounds "$TIMELINE_RIGHT_XML")"
if [ "$TIMELINE_RIGHT_FOCUS" = "$TIMELINE_FOCUS" ]; then
  echo "El foco no avanzo al siguiente programa de la timeline" >&2
  exit 1
fi
echo "Foco desplazado en timeline: $TIMELINE_RIGHT_FOCUS"

send_key KEYCODE_BACK
send_key KEYCODE_BACK
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" \
  --es reminder_channel_id "$PLAYBACK_CHANNEL_ID" \
  --es reminder_action play >/dev/null
sleep "$WAIT_SECONDS"

RESUMED="$("${ADB[@]}" shell dumpsys activity activities 2>/dev/null | grep -m1 'mResumedActivity' || true)"
if [[ "$RESUMED" != *"$APP_PACKAGE/$APP_ACTIVITY"* ]]; then
  echo "La Activity principal no recupero el primer plano" >&2
  exit 1
fi

LOG_FILE="$WORK_DIR/logcat.txt"
"${ADB[@]}" logcat -d -t 4000 > "$LOG_FILE"
if grep -iE "FATAL EXCEPTION|ANR in $APP_PACKAGE|Unable to start activity" "$LOG_FILE" >/dev/null; then
  echo "Se detecto un crash o ANR durante el recorrido de foco" >&2
  grep -iE "FATAL EXCEPTION|ANR in $APP_PACKAGE|Unable to start activity" "$LOG_FILE" | tail -n 8 >&2
  exit 1
fi

echo "Navigation/focus smoke OK: Herramientas -> TV/EPG -> timeline -> retorno."
