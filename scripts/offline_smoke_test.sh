#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-192.168.93.16:5555}"
APP_PACKAGE="${APP_PACKAGE:-com.drbep.tvplayer.offline}"
APP_ACTIVITY="${APP_ACTIVITY:-com.drbep.tvplayer.MainActivity}"
WAIT_BOOT_SECONDS="${WAIT_BOOT_SECONDS:-12}"
WAIT_AFTER_KEY_SECONDS="${WAIT_AFTER_KEY_SECONDS:-1}"
LOG_LINES="${LOG_LINES:-2500}"
LOG_OUTPUT="${LOG_OUTPUT:-}"
STRICT_WARNINGS="${STRICT_WARNINGS:-0}"
TMP_LOG=""

cleanup() {
  if [ -n "$TMP_LOG" ] && [ -f "$TMP_LOG" ]; then
    rm -f "$TMP_LOG"
  fi
}
trap cleanup EXIT

usage() {
  cat <<EOF
Uso: $0 [DEVICE]

Variables opcionales:
  APP_PACKAGE              Paquete Android. Default: $APP_PACKAGE
  APP_ACTIVITY             Activity principal. Default: $APP_ACTIVITY
  WAIT_BOOT_SECONDS        Espera tras arrancar la app. Default: $WAIT_BOOT_SECONDS
  WAIT_AFTER_KEY_SECONDS   Espera entre teclas. Default: $WAIT_AFTER_KEY_SECONDS
  LOG_LINES                Lineas de logcat a revisar. Default: $LOG_LINES
  LOG_OUTPUT               Ruta opcional donde guardar el logcat revisado.
  STRICT_WARNINGS          Si vale 1, cualquier aviso diagnostico falla el smoke. Default: $STRICT_WARNINGS

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
echo "Modo estricto avisos: $STRICT_WARNINGS"

echo
echo "== Version instalada =="
"${ADB[@]}" shell dumpsys package "$APP_PACKAGE" 2>/dev/null \
  | grep -E "versionName=|versionCode=" \
  | head -n 4 \
  | sed 's/^[[:space:]]*/  /' || echo "  no se pudo leer version"

"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
BOOT_STARTED_AT="$(date +%s)"
"${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" >/dev/null

echo "Esperando arranque (${WAIT_BOOT_SECONDS}s)..."
sleep "$WAIT_BOOT_SECONDS"

PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "La app no sigue en ejecucion tras el arranque" >&2
  "${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|$APP_PACKAGE|crash" || true
  exit 1
fi
BOOT_ELAPSED="$(( $(date +%s) - BOOT_STARTED_AT ))"
echo "PID activo tras arranque: $PID (${BOOT_ELAPSED}s)"

send_key() {
  local label="$1"
  local keycode="$2"
  echo "Tecla: $label ($keycode)"
  "${ADB[@]}" shell input keyevent "$keycode" >/dev/null
  sleep "$WAIT_AFTER_KEY_SECONDS"
}

print_log_summary() {
  local log_file="$1"
  echo
  echo "== Resumen diagnostico =="
  echo "Activity visible:"
  "${ADB[@]}" shell dumpsys window windows 2>/dev/null \
    | grep -E "mCurrentFocus|mFocusedApp" \
    | tail -n 4 \
    | sed 's/^[[:space:]]*/  /' || true
  echo
  echo "Catalogo/arranque:"
  grep -iE "startup (catalog|parsed|playback)|startup-load|startup-refresh|startup-hydrate|catalog .*duration|parse.*duration" "$log_file" \
    | tail -n 12 \
    | sed 's/^/  /' || echo "  sin metricas recientes"
  echo
  echo "EPG:"
  grep -iE "EPG|timeline|guide" "$log_file" \
    | grep -ivE "ResourcesCompat|ViewRootImpl" \
    | tail -n 12 \
    | sed 's/^/  /' || echo "  sin eventos EPG recientes"
  echo
  echo "Playback:"
  grep -iE "playChannel|prepareMediaSource|Playback route|decision=|first frame|STATE_READY|Source error|ExoPlaybackException|playback_health|rebuffer|buffering" "$log_file" \
    | tail -n 16 \
    | sed 's/^/  /' || echo "  sin eventos de reproduccion recientes"
}

count_matches() {
  local pattern="$1"
  local log_file="$2"
  local count
  count="$(grep -iE -c "$pattern" "$log_file" 2>/dev/null || true)"
  echo "${count:-0}"
}

print_warning_sample() {
  local title="$1"
  local pattern="$2"
  local log_file="$3"
  local count
  count="$(count_matches "$pattern" "$log_file")"
  if [ "$count" -gt 0 ]; then
    echo "  WARN $title: $count coincidencia(s)"
    grep -iE "$pattern" "$log_file" \
      | tail -n 4 \
      | sed 's/^/    /'
    return 1
  fi
  echo "  OK   $title"
  return 0
}

print_absence_warning() {
  local title="$1"
  local pattern="$2"
  local log_file="$3"
  local count
  count="$(count_matches "$pattern" "$log_file")"
  if [ "$count" -eq 0 ]; then
    echo "  WARN $title: sin coincidencias"
    return 1
  fi
  echo "  OK   $title: $count coincidencia(s)"
  return 0
}

print_health_gates() {
  local log_file="$1"
  local warnings=0
  echo
  echo "== Senales de salud =="
  print_warning_sample "crashes graves" "FATAL EXCEPTION|AndroidRuntime|Process: $APP_PACKAGE|Unable to start activity|ANR in $APP_PACKAGE" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "errores de reproduccion" "Source error|ExoPlaybackException|Playback error|Error de reproduccion" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "catalogo reducido/rechazado" "catalogo candidato reducido|candidate reduced|last rejected|verification warning|caller-provided IV not permitted" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "EPG con error" "epg.*error|timeline.*error|guide.*error|EPG 0 / 0" "$log_file" || warnings=$((warnings + 1))
  print_absence_warning "player listo o primer frame" "first frame|STATE_READY|playback_health" "$log_file" || warnings=$((warnings + 1))
  print_absence_warning "metricas de catalogo/arranque" "startup catalog|startup parsed|startup playback|startup-load|startup-hydrate|parse.*duration" "$log_file" || warnings=$((warnings + 1))

  if [ "$warnings" -gt 0 ]; then
    echo "Avisos diagnosticos: $warnings"
    if [ "$STRICT_WARNINGS" = "1" ]; then
      echo "STRICT_WARNINGS=1: el smoke falla por avisos diagnosticos." >&2
      return 1
    fi
  else
    echo "Sin avisos diagnosticos."
  fi
  return 0
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

TMP_LOG="$(mktemp "${TMPDIR:-/tmp}/drbep-offline-smoke.XXXXXX.log")"
"${ADB[@]}" logcat -d -t "$LOG_LINES" > "$TMP_LOG" || true

if [ -n "$LOG_OUTPUT" ]; then
  cp "$TMP_LOG" "$LOG_OUTPUT"
  echo "Log guardado en: $LOG_OUTPUT"
fi

CRASH_LOG="$(grep -iE "FATAL EXCEPTION|AndroidRuntime|Process: $APP_PACKAGE|Unable to start activity|ANR in $APP_PACKAGE" "$TMP_LOG" || true)"
if [ -n "$CRASH_LOG" ]; then
  echo "Se han detectado errores graves en logcat:" >&2
  echo "$CRASH_LOG" >&2
  print_log_summary "$TMP_LOG" >&2
  exit 1
fi

print_log_summary "$TMP_LOG"
print_health_gates "$TMP_LOG"
echo "Smoke test OK: app viva y sin FATAL/AndroidRuntime recientes."
