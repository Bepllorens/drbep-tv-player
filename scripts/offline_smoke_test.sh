#!/usr/bin/env bash
set -euo pipefail

DEVICE="${1:-192.168.93.189:5555}"
APP_PACKAGE="${APP_PACKAGE:-com.drbep.tvplayer.offline}"
APP_ACTIVITY="${APP_ACTIVITY:-com.drbep.tvplayer.MainActivity}"
WAIT_BOOT_SECONDS="${WAIT_BOOT_SECONDS:-12}"
WAIT_AFTER_KEY_SECONDS="${WAIT_AFTER_KEY_SECONDS:-1}"
WAIT_BACKGROUND_SECONDS="${WAIT_BACKGROUND_SECONDS:-4}"
LOG_LINES="${LOG_LINES:-8000}"
LOG_OUTPUT="${LOG_OUTPUT:-}"
REPORT_OUTPUT="${REPORT_OUTPUT:-}"
STRICT_WARNINGS="${STRICT_WARNINGS:-0}"
CHECK_GUIDE_KEY="${CHECK_GUIDE_KEY:-1}"
CHECK_BACKGROUND="${CHECK_BACKGROUND:-1}"
PLAYBACK_CHANNEL_ID="${PLAYBACK_CHANNEL_ID:-}"
TMP_LOG=""
BACKGROUND_PROCESS_KEPT="unknown"

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
  WAIT_BACKGROUND_SECONDS Espera al salir al launcher. Default: $WAIT_BACKGROUND_SECONDS
  LOG_LINES                Lineas de logcat a revisar. Default: $LOG_LINES
  LOG_OUTPUT               Ruta opcional donde guardar el logcat revisado.
  REPORT_OUTPUT            Ruta opcional del informe JSON estructurado.
  STRICT_WARNINGS          Si vale 1, cualquier aviso diagnostico falla el smoke. Default: $STRICT_WARNINGS
  CHECK_GUIDE_KEY          Si vale 1, pulsa KEYCODE_GUIDE durante el smoke. Default: $CHECK_GUIDE_KEY
  CHECK_BACKGROUND         Si vale 1, valida salida y retorno sin reiniciar proceso. Default: $CHECK_BACKGROUND
  PLAYBACK_CHANNEL_ID      ID opcional de un canal estable que debe alcanzar primer frame.

Ejemplos:
  $0 192.168.93.189:5555
  $0 ZY32JB8XR3
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

if [ -n "$PLAYBACK_CHANNEL_ID" ] && [[ "$PLAYBACK_CHANNEL_ID" == *[!0-9]* ]]; then
  echo "PLAYBACK_CHANNEL_ID debe ser un ID numerico de canal" >&2
  exit 1
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
echo "Comprueba GUIDE: $CHECK_GUIDE_KEY"
echo "Comprueba segundo plano: $CHECK_BACKGROUND"
echo "Canal de reproduccion: ${PLAYBACK_CHANNEL_ID:-canal previo y zapping relativo}"

echo
echo "== Version instalada =="
"${ADB[@]}" shell dumpsys package "$APP_PACKAGE" 2>/dev/null \
  | grep -E "versionName=|versionCode=" \
  | head -n 4 \
  | sed 's/^[[:space:]]*/  /' || echo "  no se pudo leer version"

"${ADB[@]}" shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
sleep 1
"${ADB[@]}" logcat -c || true
BOOT_STARTED_AT="$(date +%s)"
START_ARGS=(-n "$APP_PACKAGE/$APP_ACTIVITY")
if [ -n "$PLAYBACK_CHANNEL_ID" ]; then
  START_ARGS+=(--es reminder_channel_id "$PLAYBACK_CHANNEL_ID" --es reminder_action play)
fi
"${ADB[@]}" shell am start "${START_ARGS[@]}" >/dev/null

echo "Esperando arranque (${WAIT_BOOT_SECONDS}s)..."
PID=""
while [ -z "$PID" ] && [ "$(( $(date +%s) - BOOT_STARTED_AT ))" -lt "$WAIT_BOOT_SECONDS" ]; do
  sleep 1
  PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
done
if [ -z "$PID" ]; then
  echo "La app no sigue en ejecucion tras el arranque" >&2
  "${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|$APP_PACKAGE|crash" || true
  exit 1
fi
BOOT_ELAPSED="$(( $(date +%s) - BOOT_STARTED_AT ))"
REMAINING_BOOT_WAIT="$((WAIT_BOOT_SECONDS - BOOT_ELAPSED))"
if [ "$REMAINING_BOOT_WAIT" -gt 0 ]; then
  sleep "$REMAINING_BOOT_WAIT"
fi
echo "PID detectado tras arranque: $PID (${BOOT_ELAPSED}s; estabilizacion ${WAIT_BOOT_SECONDS}s)"

send_key() {
  local label="$1"
  local keycode="$2"
  echo "Tecla: $label ($keycode)"
  "${ADB[@]}" shell input keyevent "$keycode" >/dev/null
  sleep "$WAIT_AFTER_KEY_SECONDS"
}

ensure_main_activity() {
  local resumed
  resumed="$("${ADB[@]}" shell dumpsys activity activities 2>/dev/null | grep -m1 "mResumedActivity" || true)"
  if [[ "$resumed" != *"$APP_PACKAGE/$APP_ACTIVITY"* ]]; then
    echo "Restaurando Activity principal para continuar el smoke"
    "${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" >/dev/null
    sleep "$WAIT_AFTER_KEY_SECONDS"
  fi
}

print_log_summary() {
  local log_file="$1"
  echo
  echo "== Resumen diagnostico =="
  echo "Activity visible:"
  "${ADB[@]}" shell dumpsys activity activities 2>/dev/null \
    | grep -E "mResumedActivity|topResumedActivity" \
    | tail -n 4 \
    | sed 's/^[[:space:]]*/  /' || true
  echo
  echo "Catalogo/arranque:"
  grep -iE "startup (catalog|parsed|playback)|startup-load|startup-refresh|startup-hydrate|catalog .*duration|parse.*duration" "$log_file" \
    | tail -n 12 \
    | redact_log_lines \
    | sed 's/^/  /' || echo "  sin metricas recientes"
  echo
  echo "EPG:"
  grep -iE "EPG|timeline|guide" "$log_file" \
    | grep -ivE "ResourcesCompat|ViewRootImpl" \
    | tail -n 12 \
    | redact_log_lines \
    | sed 's/^/  /' || echo "  sin eventos EPG recientes"
  echo
  echo "Playback:"
  grep -iE "playChannel|prepareMediaSource|Playback route|decision=|first frame|STATE_READY|Source error|ExoPlaybackException|playback_health|rebuffer|buffering" "$log_file" \
    | tail -n 16 \
    | redact_log_lines \
    | sed 's/^/  /' || echo "  sin eventos de reproduccion recientes"
}

redact_log_lines() {
  sed -E \
    -e 's#https?://[^,}[:space:]]+#<url>#g' \
    -e 's#([,?&[:space:]])([Tt]oken|[Aa]pi[_-]?[Kk]ey|[Kk]ey|[Aa]uthorization)=?[^,}[:space:]]+#\1\2=<redactado>#g'
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
      | redact_log_lines \
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
  local playback_error_label="errores de reproduccion"
  local playback_error_pattern="Source error|ExoPlaybackException|Playback error|Error de reproduccion"
  local first_frame_label="player listo o primer frame"
  local first_frame_pattern="first frame|STATE_READY|playback_health"
  if [ -n "$PLAYBACK_CHANNEL_ID" ]; then
    playback_error_label="errores del canal fijado $PLAYBACK_CHANNEL_ID"
    playback_error_pattern="onPlayerError channel=\\{$PLAYBACK_CHANNEL_ID,"
    first_frame_label="primer frame del canal fijado $PLAYBACK_CHANNEL_ID"
    first_frame_pattern="firstFrame channel=\\{$PLAYBACK_CHANNEL_ID,"
  fi
  echo
  echo "== Senales de salud =="
  print_warning_sample "crashes graves" "FATAL EXCEPTION|Process: $APP_PACKAGE|Unable to start activity|ANR in $APP_PACKAGE" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "$playback_error_label" "$playback_error_pattern" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "catalogo reducido/rechazado" "catalogo candidato reducido|candidate reduced|last rejected|verification warning|caller-provided IV not permitted" "$log_file" || warnings=$((warnings + 1))
  print_warning_sample "EPG con error" "epg.*error|timeline.*error|guide.*error|EPG 0 / 0" "$log_file" || warnings=$((warnings + 1))
  print_absence_warning "$first_frame_label" "$first_frame_pattern" "$log_file" || warnings=$((warnings + 1))
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
ensure_main_activity
send_key "Info / Guia actual" KEYCODE_INFO
send_key "Back cerrar panel" KEYCODE_BACK
ensure_main_activity
if [ "$CHECK_GUIDE_KEY" = "1" ]; then
  send_key "Guia timeline" KEYCODE_GUIDE
  send_key "Back cerrar guia" KEYCODE_BACK
  ensure_main_activity
fi
send_key "Menu herramientas" KEYCODE_MENU
send_key "Back cerrar menu" KEYCODE_BACK
ensure_main_activity
send_key "Grabacion" KEYCODE_MEDIA_RECORD
send_key "Back cerrar grabacion" KEYCODE_BACK
ensure_main_activity
if [ -n "$PLAYBACK_CHANNEL_ID" ]; then
  echo "Zapping relativo omitido: la senal se valida sobre el canal fijado $PLAYBACK_CHANNEL_ID"
else
  send_key "Canal abajo" KEYCODE_DPAD_DOWN
  send_key "Canal arriba" KEYCODE_DPAD_UP
fi

if [ "$CHECK_BACKGROUND" = "1" ]; then
  PID_BEFORE_BACKGROUND="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  send_key "Launcher / segundo plano" KEYCODE_HOME
  sleep "$WAIT_BACKGROUND_SECONDS"
  PID_DURING_BACKGROUND="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  "${ADB[@]}" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" >/dev/null
  sleep "$WAIT_AFTER_KEY_SECONDS"
  PID_AFTER_BACKGROUND="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
  if [ -n "$PID_BEFORE_BACKGROUND" ] \
      && [ "$PID_BEFORE_BACKGROUND" = "$PID_DURING_BACKGROUND" ] \
      && [ "$PID_BEFORE_BACKGROUND" = "$PID_AFTER_BACKGROUND" ]; then
    BACKGROUND_PROCESS_KEPT="true"
    echo "Proceso conservado al volver del launcher: $PID_AFTER_BACKGROUND"
  else
    BACKGROUND_PROCESS_KEPT="false"
    echo "La app reinicio o perdio el proceso al pasar por segundo plano" >&2
  fi
fi

PID="$("${ADB[@]}" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r' || true)"
if [ -z "$PID" ]; then
  echo "La app se ha cerrado durante el smoke test" >&2
  "${ADB[@]}" logcat -d -t "$LOG_LINES" | grep -iE "FATAL EXCEPTION|AndroidRuntime|$APP_PACKAGE|crash" || true
  exit 1
fi

TMP_LOG="$(mktemp "${TMPDIR:-/tmp}/drbep-offline-smoke.XXXXXX.log")"
"${ADB[@]}" logcat -d --pid="$PID" -t "$LOG_LINES" > "$TMP_LOG" || true

if [ -n "$LOG_OUTPUT" ]; then
  cp "$TMP_LOG" "$LOG_OUTPUT"
  echo "Log guardado en: $LOG_OUTPUT"
fi

CRASH_LOG="$(grep -iE "FATAL EXCEPTION|Process: $APP_PACKAGE|Unable to start activity|ANR in $APP_PACKAGE" "$TMP_LOG" || true)"
if [ -n "$CRASH_LOG" ]; then
  echo "Se han detectado errores graves en logcat:" >&2
  echo "$CRASH_LOG" >&2
  print_log_summary "$TMP_LOG" >&2
  exit 1
fi

print_log_summary "$TMP_LOG"
SMOKE_OK=1
if ! print_health_gates "$TMP_LOG"; then
  SMOKE_OK=0
fi

if [ "$BACKGROUND_PROCESS_KEPT" = "false" ]; then
  SMOKE_OK=0
fi

if [ -n "$REPORT_OUTPUT" ]; then
  DEVICE_MODEL="$("${ADB[@]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
  VERSION_CODE="$("${ADB[@]}" shell dumpsys package "$APP_PACKAGE" 2>/dev/null | sed -n 's/.*versionCode=\([0-9][0-9]*\).*/\1/p' | head -n 1)"
  VERSION_NAME="$("${ADB[@]}" shell dumpsys package "$APP_PACKAGE" 2>/dev/null | sed -n 's/.*versionName=\([^[:space:]]*\).*/\1/p' | head -n 1)"
  TOTAL_PSS_KB="$("${ADB[@]}" shell dumpsys meminfo "$APP_PACKAGE" 2>/dev/null | sed -n 's/.*TOTAL PSS:[[:space:]]*\([0-9][0-9]*\).*/\1/p' | head -n 1)"
  python3 "$(dirname "$0")/offline_smoke_report.py" \
    --log "$TMP_LOG" \
    --output "$REPORT_OUTPUT" \
    --device "$DEVICE" \
    --model "$DEVICE_MODEL" \
    --version-code "${VERSION_CODE:-0}" \
    --version-name "$VERSION_NAME" \
    --pid "$PID" \
    --process-detected-seconds "$BOOT_ELAPSED" \
    --total-pss-kb "${TOTAL_PSS_KB:-0}" \
    --background-process-kept "$BACKGROUND_PROCESS_KEPT" \
    --smoke-ok "$SMOKE_OK"
  echo "Informe JSON guardado en: $REPORT_OUTPUT"
fi

if [ "$SMOKE_OK" != "1" ]; then
  echo "Smoke test con fallos de salud o ciclo de vida." >&2
  exit 1
fi
echo "Smoke test OK: app viva y sin FATAL/AndroidRuntime recientes."
