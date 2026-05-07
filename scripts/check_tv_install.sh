#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Uso: $0 <IP_TV[:PUERTO]>"
  exit 1
fi

TV_ADDR="$1"
APP_PACKAGE="com.drbep.tvplayer.firestick"
LEGACY_PACKAGES=(
  "com.drbep.tvplayer.firetv"
  "com.drbep.tvplayer"
  "com.drbep.tv.v2.fixed"
)

adb connect "$TV_ADDR" >/dev/null
ADB=(adb -s "$TV_ADDR")

echo "== Paquete principal =="
"${ADB[@]}" shell dumpsys package "$APP_PACKAGE" | grep -E "Package \\[|versionCode|versionName|lastUpdateTime|enabled=" || true

echo
echo "== Paquetes DRBEP instalados =="
"${ADB[@]}" shell pm list packages | grep -Ei "drbep|tvplayer|bep" || true

echo
echo "== Paquetes antiguos conocidos =="
for legacy_package in "${LEGACY_PACKAGES[@]}"; do
  if "${ADB[@]}" shell pm path "$legacy_package" >/dev/null 2>&1; then
    echo "$legacy_package instalado"
    "${ADB[@]}" shell dumpsys package "$legacy_package" | grep -E "versionCode|versionName|lastUpdateTime|enabled=" || true
  else
    echo "$legacy_package no instalado"
  fi
done

echo
echo "== Activity activa =="
"${ADB[@]}" shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity|com\\.drbep" || true
