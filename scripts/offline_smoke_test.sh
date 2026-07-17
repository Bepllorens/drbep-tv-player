#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-192.168.93.16:5555}"
APP_PACKAGE="${APP_PACKAGE:-com.drbep.tvplayer.offline}"
APP_ACTIVITY="${APP_ACTIVITY:-com.drbep.tvplayer.MainActivity}"
WAIT_BOOT_SECONDS="${WAIT_BOOT_SECONDS:-12}"
WAIT_AFTER_KEY_SECONDS="${WAIT_AFTER_KEY_SECONDS:-1}"
LOG_LINES="${LOG_LINES:-2500}"

usage() {
  cat <<EOF
Uso: $0 [DEVICE]

Variables opcionales:
  APP_PACKAGE              Paquete Android. Default: $APP_PACKAGE
  APP_ACTIVITY             Activity principal. Default: $APP_ACTIVITY
  WAIT_BOOT_SECONDS        Espera tras arrancar la app. Default: $WAIT_BOOT_SECONDS
  WAIT_AFTER_KEY_SECONDS   Espera entre teclas. Default: $WAIT_AFTER_KEY_SECONDS
  LOG_LINES                Lineas de logcat a revisar. Default: $LOG_LINES

Ejemplos:
  $0 192.168.93.16:5555
  $0 ZY32JB8XR3
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

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

echo "== DRBEP offline smoke test =="
echo "Dispositivo: $DEVICE"
echo "Paquete:     $APP_PACKAGE"
echo "Activity:    $APP_ACTIVITY"

"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" >/dev/null

echo "Esperando arranque (${WAIT_BOOT_SECONDS}s)..."
sleep "$WAIT_BOOT_SECONDS"

PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "La app no sigue en ejecucion tras el arranque" >&2
  "${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|$APP_PACKAGE|crash" || true
  exit 1
fi

send_key() {
  local label="$1"
  local keycode="$2"
  echo "Tecla: $label ($keycode)"
  "${ADB[@]}" shell input keyevent "$keycode" >/dev/null
  sleep "$WAIT_AFTER_KEY_SECONDS"
}

# Secuencia conservadora: abrir HUD, navegar botones, abrir/cerrar guia/info/grabaciones
# sin asumir un canal o plataforma concretos.
send_key "OK / HUD inferior" KEYCODE_DPAD_CENTER
send_key "Derecha en HUD" KEYCODE_DPAD_RIGHT
send_key "Derecha en HUD" KEYCODE_DPAD_RIGHT
send_key "Izquierda en HUD" KEYCODE_DPAD_LEFT
send_key "Arriba hacia timeshift" KEYCODE_DPAD_UP
send_key "Abajo hacia acciones" KEYCODE_DPAD_DOWN
send_key "Back cerrar HUD" KEYCODE_BACK
send_key "Info / Guia actual" KEYCODE_INFO
send_key "Back cerrar panel" KEYCODE_BACK
send_key "Menu herramientas" KEYCODE_MENU
send_key "Back cerrar menu" KEYCODE_BACK
send_key "Grabacion" KEYCODE_MEDIA_RECORD
send_key "Back cerrar grabacion" KEYCODE_BACK
send_key "Canal abajo" KEYCODE_DPAD_DOWN
send_key "Canal arriba" KEYCODE_DPAD_UP

PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "La app se ha cerrado durante el smoke test" >&2
  "${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|$APP_PACKAGE|crash" || true
  exit 1
fi

CRASH_LOG="$("${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|Process: $APP_PACKAGE|Unable to start activity|ANR in $APP_PACKAGE" || true)"
if [ -n "$CRASH_LOG" ]; then
  echo "Se han detectado errores graves en logcat:" >&2
  echo "$CRASH_LOG" >&2
  exit 1
fi

echo "Smoke test OK: app viva y sin FATAL/AndroidRuntime recientes."
