#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SIGNING_PROPS="${DRBEP_RELEASE_SIGNING_PROPERTIES:-$HOME/.gradle/drbep-tv-player-release.properties}"

if [[ ! -f "$SIGNING_PROPS" ]]; then
  echo "Missing release signing properties: $SIGNING_PROPS" >&2
  echo "Create it with DRBEP_RELEASE_STORE_FILE, DRBEP_RELEASE_STORE_PASSWORD, DRBEP_RELEASE_KEY_ALIAS and DRBEP_RELEASE_KEY_PASSWORD." >&2
  exit 1
fi

set -a
source "$SIGNING_PROPS"
set +a

cd "$ROOT_DIR"
./gradlew --no-daemon --max-workers=2 :app:assembleRelease "$@"

APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
  echo "Release APK not found: $APK" >&2
  exit 1
fi

sha256sum "$APK" 2>/dev/null || shasum -a 256 "$APK"
