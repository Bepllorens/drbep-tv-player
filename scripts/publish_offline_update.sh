#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHANNEL="${DRBEP_UPDATE_CHANNEL:-stable}"
BASE_URL="${DRBEP_API_BASE_URL:-http://127.0.0.1:8080}"
PUBLIC_BASE_URL="${DRBEP_PUBLIC_BASE_URL:-https://iptv.bepllorens.com}"
API_RELEASE_DIR="${DRBEP_OFFLINE_API_RELEASE_DIR:-/data/offline-app-releases}"
HOST_RELEASE_DIR="${DRBEP_OFFLINE_HOST_RELEASE_DIR:-}"
EXPECTED_CERT_SHA256="${DRBEP_RELEASE_EXPECTED_CERT_SHA256:-0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b}"
REQUIRED_APK_ABIS="${DRBEP_REQUIRED_APK_ABIS:-armeabi-v7a,arm64-v8a}"
CHANGELOG_TEXT="${DRBEP_UPDATE_CHANGELOG:-Build release publicado desde el servidor.}"
REQUIRED="${DRBEP_UPDATE_REQUIRED:-0}"
UPDATE_ENABLED="${DRBEP_UPDATE_ENABLED:-1}"
DRY_RUN=0
PREBUILT_APK=""

BUILD_ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --channel)
      CHANNEL="${2:-}"
      shift 2
      ;;
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --public-base-url)
      PUBLIC_BASE_URL="${2:-}"
      shift 2
      ;;
    --release-dir)
      RELEASE_DIR="${2:-}"
      shift 2
      ;;
    --changelog)
      CHANGELOG_TEXT="${2:-}"
      shift 2
      ;;
    --apk)
      PREBUILT_APK="${2:-}"
      shift 2
      ;;
    --required)
      REQUIRED=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    --)
      shift
      BUILD_ARGS+=("$@")
      break
      ;;
    *)
      BUILD_ARGS+=("$1")
      shift
      ;;
  esac
done

case "$CHANNEL" in
  stable|beta|rescue) ;;
  *)
    echo "Invalid channel: $CHANNEL" >&2
    exit 1
    ;;
esac

cd "$ROOT_DIR"
if [[ -n "$PREBUILT_APK" ]]; then
  if [[ "$PREBUILT_APK" = /* ]]; then
    APK="$PREBUILT_APK"
  else
    APK="$ROOT_DIR/$PREBUILT_APK"
  fi
  if [[ ! -f "$APK" ]]; then
    echo "Prebuilt APK not found: $APK" >&2
    exit 1
  fi
else
  if [[ ${#BUILD_ARGS[@]} -gt 0 ]]; then
    DRBEP_RELEASE_EXPECTED_CERT_SHA256="$EXPECTED_CERT_SHA256" scripts/build_release_apk.sh "${BUILD_ARGS[@]}"
  else
    DRBEP_RELEASE_EXPECTED_CERT_SHA256="$EXPECTED_CERT_SHA256" scripts/build_release_apk.sh
  fi
  APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
fi

VERSION_CODE="$(awk '/versionCode/ {print $2; exit}' app/build.gradle)"
VERSION_NAME="$(awk -F'"' '/versionName/ {print $2; exit}' app/build.gradle)"
SHA256="$(sha256sum "$APK" 2>/dev/null | awk '{print $1}')"
if [[ -z "$SHA256" ]]; then
  SHA256="$(shasum -a 256 "$APK" | awk '{print $1}')"
fi

APKSIGNER="${APKSIGNER:-$(command -v apksigner || true)}"
if [[ -z "$APKSIGNER" ]]; then
  echo "apksigner not found; cannot verify release certificate" >&2
  exit 1
fi
AAPT="${AAPT:-$(command -v aapt || true)}"
if [[ -z "$AAPT" && -n "$APKSIGNER" && -x "$(dirname "$APKSIGNER")/aapt" ]]; then
  AAPT="$(dirname "$APKSIGNER")/aapt"
fi
if [[ -z "$AAPT" ]]; then
  echo "aapt not found; cannot verify APK version" >&2
  exit 1
fi
APK_BADGING="$("$AAPT" dump badging "$APK")"
APK_PACKAGE="$(printf '%s\n' "$APK_BADGING" | head -n 1)"
APK_NATIVE_CODE="$(printf '%s\n' "$APK_BADGING" | sed -n "s/^native-code: //p")"
APK_VERSION_CODE="$(printf '%s\n' "$APK_PACKAGE" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p")"
APK_VERSION_NAME="$(printf '%s\n' "$APK_PACKAGE" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
IFS=',' read -r -a REQUIRED_ABI_LIST <<< "$REQUIRED_APK_ABIS"
for REQUIRED_ABI in "${REQUIRED_ABI_LIST[@]}"; do
  REQUIRED_ABI="$(printf '%s' "$REQUIRED_ABI" | xargs)"
  if [[ -n "$REQUIRED_ABI" && "$APK_NATIVE_CODE" != *"'$REQUIRED_ABI'"* ]]; then
    echo "APK ABI mismatch: required $REQUIRED_ABI; APK reports ${APK_NATIVE_CODE:-no native code}" >&2
    exit 1
  fi
done
if [[ "$APK_VERSION_CODE" != "$VERSION_CODE" || "$APK_VERSION_NAME" != "$VERSION_NAME" ]]; then
  echo "APK version mismatch: expected $VERSION_CODE $VERSION_NAME, got $APK_VERSION_CODE $APK_VERSION_NAME" >&2
  exit 1
fi
CERT_SHA256="$("$APKSIGNER" verify --print-certs "$APK" | awk -F'digest: ' '/SHA-256 digest/ {print tolower($2); exit}' | tr -d ':[:space:]')"
EXPECTED_NORMALIZED="$(printf '%s' "$EXPECTED_CERT_SHA256" | tr '[:upper:]' '[:lower:]' | tr -d ':[:space:]')"
if [[ "$CERT_SHA256" != "$EXPECTED_NORMALIZED" ]]; then
  echo "Release certificate mismatch: expected $EXPECTED_NORMALIZED, got $CERT_SHA256" >&2
  exit 1
fi

STAMP="$(date +%Y%m%d_%H%M%S)"
DEST_NAME="drbep-tv-player-offline-${CHANNEL}-v${VERSION_CODE}-${STAMP}.apk"
API_DEST="$API_RELEASE_DIR/$DEST_NAME"
if [[ -z "$HOST_RELEASE_DIR" ]]; then
  if command -v docker >/dev/null 2>&1; then
    DATA_VOLUME="$(docker inspect drbep --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)"
    if [[ -n "$DATA_VOLUME" && -d "$DATA_VOLUME" ]]; then
      HOST_RELEASE_DIR="$DATA_VOLUME/offline-app-releases"
    fi
  fi
fi
if [[ -z "$HOST_RELEASE_DIR" ]]; then
  HOST_RELEASE_DIR="$API_RELEASE_DIR"
fi
HOST_DEST="$HOST_RELEASE_DIR/$DEST_NAME"
APK_URL="${PUBLIC_BASE_URL%/}/api/offline/app/apk"
if [[ "$CHANNEL" != "stable" ]]; then
  APK_URL="${APK_URL}?channel=${CHANNEL}"
fi

echo "Version: $VERSION_CODE $VERSION_NAME"
echo "SHA-256: $SHA256"
echo "Certificate: $CERT_SHA256"
echo "Native code: ${APK_NATIVE_CODE:-none}"
echo "Destination: $API_DEST"
echo "Host copy: $HOST_DEST"
echo "Channel: $CHANNEL"
if [[ -n "$PREBUILT_APK" ]]; then
  echo "Build: skipped; publishing verified prebuilt APK"
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Dry-run: not copying APK and not publishing update metadata."
  exit 0
fi

install -d -m 0755 "$HOST_RELEASE_DIR"
install -m 0644 "$APK" "$HOST_DEST"

REQUEST_JSON="$(mktemp)"
DRBEP_PUBLISH_CHANNEL="$CHANNEL" \
DRBEP_PUBLISH_UPDATE_ENABLED="$UPDATE_ENABLED" \
DRBEP_PUBLISH_REQUIRED="$REQUIRED" \
DRBEP_PUBLISH_VERSION_CODE="$VERSION_CODE" \
DRBEP_PUBLISH_VERSION_NAME="$VERSION_NAME" \
DRBEP_PUBLISH_APK_URL="$APK_URL" \
DRBEP_PUBLISH_API_DEST="$API_DEST" \
DRBEP_PUBLISH_SHA256="$SHA256" \
DRBEP_PUBLISH_CHANGELOG="$CHANGELOG_TEXT" \
python3 - "$REQUEST_JSON" <<'PY'
import json
import os
import sys

path = sys.argv[1]
payload = {
    "channel": os.environ["DRBEP_PUBLISH_CHANNEL"],
    "update_enabled": os.environ["DRBEP_PUBLISH_UPDATE_ENABLED"] not in ("0", "false", "False"),
    "required": os.environ["DRBEP_PUBLISH_REQUIRED"] not in ("0", "false", "False"),
    "version_code": int(os.environ["DRBEP_PUBLISH_VERSION_CODE"]),
    "version_name": os.environ["DRBEP_PUBLISH_VERSION_NAME"],
    "apk_url": os.environ["DRBEP_PUBLISH_APK_URL"],
    "apk_path": os.environ["DRBEP_PUBLISH_API_DEST"],
    "sha256": os.environ["DRBEP_PUBLISH_SHA256"],
    "changelog_text": os.environ["DRBEP_PUBLISH_CHANGELOG"],
}
with open(path, "w", encoding="utf-8") as fh:
    json.dump(payload, fh)
PY

HEADERS=(-H "Content-Type: application/json" -H "X-Ops-Profile: admin" -H "X-Ops-Override: 1")
if [[ -n "${DRBEP_ADMIN_API_KEY:-}" ]]; then
  HEADERS+=(-H "X-API-Key: ${DRBEP_ADMIN_API_KEY}")
fi

curl -fsS "${HEADERS[@]}" -d "@$REQUEST_JSON" "${BASE_URL%/}/api/offline/app/publish"
rm -f "$REQUEST_JSON"
echo
echo "Published offline app update $VERSION_CODE ($VERSION_NAME) to $CHANNEL."
