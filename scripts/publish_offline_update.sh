#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHANNEL="${DRBEP_UPDATE_CHANNEL:-stable}"
BASE_URL="${DRBEP_API_BASE_URL:-http://127.0.0.1:8080}"
PUBLIC_BASE_URL="${DRBEP_PUBLIC_BASE_URL:-https://iptv.bepllorens.com}"
API_RELEASE_DIR="${DRBEP_OFFLINE_API_RELEASE_DIR:-/data/offline-app-releases}"
HOST_RELEASE_DIR="${DRBEP_OFFLINE_HOST_RELEASE_DIR:-}"
EXPECTED_CERT_SHA256="${DRBEP_RELEASE_EXPECTED_CERT_SHA256:-0ddf793032d3f3a9c3a3939e9a501719e15d3f5c5b0a3e10b33eac01b412e34b}"
CHANGELOG_TEXT="${DRBEP_UPDATE_CHANGELOG:-Build release publicado desde el servidor.}"
REQUIRED="${DRBEP_UPDATE_REQUIRED:-0}"
UPDATE_ENABLED="${DRBEP_UPDATE_ENABLED:-1}"
DRY_RUN=0

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
if [[ ${#BUILD_ARGS[@]} -gt 0 ]]; then
  DRBEP_RELEASE_EXPECTED_CERT_SHA256="$EXPECTED_CERT_SHA256" scripts/build_release_apk.sh "${BUILD_ARGS[@]}"
else
  DRBEP_RELEASE_EXPECTED_CERT_SHA256="$EXPECTED_CERT_SHA256" scripts/build_release_apk.sh
fi

APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
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
echo "Destination: $API_DEST"
echo "Host copy: $HOST_DEST"
echo "Channel: $CHANNEL"

if [[ "$DRY_RUN" == "1" ]]; then
  echo "Dry-run: not copying APK and not publishing update metadata."
  exit 0
fi

install -d -m 0755 "$HOST_RELEASE_DIR"
install -m 0644 "$APK" "$HOST_DEST"

REQUEST_JSON="$(mktemp)"
python3 - "$REQUEST_JSON" <<PY
import json
import sys

path = sys.argv[1]
payload = {
    "channel": "$CHANNEL",
    "update_enabled": "$UPDATE_ENABLED" not in ("0", "false", "False"),
    "required": "$REQUIRED" not in ("0", "false", "False"),
    "version_code": int("$VERSION_CODE"),
    "version_name": "$VERSION_NAME",
    "apk_url": "$APK_URL",
    "apk_path": "$API_DEST",
    "sha256": "$SHA256",
    "changelog_text": "$CHANGELOG_TEXT",
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
