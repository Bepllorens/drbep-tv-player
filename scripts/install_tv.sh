#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Uso: $0 <IP_TV[:PUERTO]>"
  exit 1
fi

TV_ADDR="$1"
APK_PATH="$(cd "$(dirname "$0")/.." && pwd)/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
  echo "No existe APK en: $APK_PATH"
  echo "Primero compila con scripts/build_apk.sh"
  exit 1
fi

adb connect "$TV_ADDR"
adb install -r "$APK_PATH"
echo "Instalado en $TV_ADDR"
