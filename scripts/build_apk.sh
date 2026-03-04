#!/usr/bin/env bash
set -euo pipefail

PLAYER_URL="${1:-https://iptv.bepllorens.com/player.html}"
ALLOW_INSECURE_SSL="${2:-true}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"

cd "$(dirname "$0")/.."
./gradlew assembleDebug -PplayerUrl="$PLAYER_URL" -PallowInsecureSsl="$ALLOW_INSECURE_SSL"

echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
