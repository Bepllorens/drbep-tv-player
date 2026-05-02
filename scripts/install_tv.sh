#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Uso: $0 <IP_TV[:PUERTO]> [APK_PATH]"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TV_ADDR="$1"
APK_PATH="${2:-$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk}"
APP_PACKAGE="com.drbep.tvplayer"
APP_ACTIVITY="$APP_PACKAGE/.MainActivity"
LEGACY_PACKAGES=(
  "com.drbep.tv.v2.fixed"
)

if [ ! -f "$APK_PATH" ]; then
  echo "No existe APK en: $APK_PATH"
  echo "Primero compila con scripts/build_apk.sh o ./gradlew assembleDebug"
  exit 1
fi

adb connect "$TV_ADDR"
adb install -r "$APK_PATH"

if [ "${KEEP_LEGACY_DRBEP_PACKAGES:-0}" != "1" ]; then
  for legacy_package in "${LEGACY_PACKAGES[@]}"; do
    if adb shell pm path "$legacy_package" >/dev/null 2>&1; then
      echo "Desactivando paquete antiguo: $legacy_package"
      adb shell am force-stop "$legacy_package" >/dev/null 2>&1 || true
      adb shell pm disable-user --user 0 "$legacy_package" >/dev/null 2>&1 || true
    fi
  done
fi

adb shell am force-stop "$APP_PACKAGE" >/dev/null 2>&1 || true
adb shell am start -n "$APP_ACTIVITY" >/dev/null

echo "Instalado y arrancado en $TV_ADDR"
adb shell dumpsys package "$APP_PACKAGE" | grep -E "versionCode|versionName|lastUpdateTime" || true
