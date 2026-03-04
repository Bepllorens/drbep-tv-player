#!/usr/bin/env bash
set -euo pipefail

LOCKED_URL="${1:-http://192.168.93.223:8080/player.html}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"

cd "$(dirname "$0")/.."
./gradlew clean assembleDebug \
  -PforceFirestickUrl=true \
  -PfirestickLockedUrl="$LOCKED_URL" \
  -PplayerUrl="$LOCKED_URL" \
  -PallowInsecureSsl=true

cp app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/debug/app-debug-firestick-fixed.apk
echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug-firestick-fixed.apk"
