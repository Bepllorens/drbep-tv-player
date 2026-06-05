#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SIGNING_PROPS="${DRBEP_RELEASE_SIGNING_PROPERTIES:-$HOME/.gradle/drbep-tv-player-release.properties}"
EXPECTED_CERT_SHA256="${DRBEP_RELEASE_EXPECTED_CERT_SHA256:-}"

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

if [[ -n "$EXPECTED_CERT_SHA256" ]]; then
  APKSIGNER="${APKSIGNER:-$(command -v apksigner || true)}"
  if [[ -z "$APKSIGNER" ]]; then
    echo "apksigner not found; cannot verify release certificate" >&2
    exit 1
  fi
  CERT_SHA256="$("$APKSIGNER" verify --print-certs "$APK" | awk -F'digest: ' '/SHA-256 digest/ {print tolower($2); exit}' | tr -d ':[:space:]')"
  EXPECTED_NORMALIZED="$(printf '%s' "$EXPECTED_CERT_SHA256" | tr '[:upper:]' '[:lower:]' | tr -d ':[:space:]')"
  if [[ "$CERT_SHA256" != "$EXPECTED_NORMALIZED" ]]; then
    echo "Release certificate mismatch: expected $EXPECTED_NORMALIZED, got $CERT_SHA256" >&2
    exit 1
  fi
  echo "Release certificate OK: $CERT_SHA256"
fi
