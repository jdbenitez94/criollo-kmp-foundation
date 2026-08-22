#!/usr/bin/env bash
# Zip the local Maven repo bundle and upload it to the Central Publisher Portal API
# as a single AUTOMATIC deployment, then poll until PUBLISHED (or fail).
#
# https://central.sonatype.org/publish/publish-portal-api/
#
# Required env:
#   RELEASE_VERSION
#   OSSRH_USERNAME, OSSRH_PASSWORD
# Optional env:
#   BUNDLE_DIR (default $PWD/build/central-bundle)
#   PORTAL_POLL_SECONDS (default 20)
#   PORTAL_TIMEOUT_SECONDS (default 2400 = 40m)
set -euo pipefail

: "${RELEASE_VERSION:?}"
: "${OSSRH_USERNAME:?}"
: "${OSSRH_PASSWORD:?}"

BUNDLE_DIR="${BUNDLE_DIR:-$PWD/build/central-bundle}"
POLL_SECONDS="${PORTAL_POLL_SECONDS:-20}"
TIMEOUT_SECONDS="${PORTAL_TIMEOUT_SECONDS:-2400}"
AUTH="$(printf '%s:%s' "$OSSRH_USERNAME" "$OSSRH_PASSWORD" | base64 | tr -d '\n')"
PORTAL_API="https://central.sonatype.com/api/v1/publisher"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

if [ ! -d "$BUNDLE_DIR" ]; then
  echo "::error title=Missing local bundle dir::${BUNDLE_DIR}"
  exit 1
fi

ZIP_PATH="${WORK_DIR}/criollo-kmp-foundation-${RELEASE_VERSION}.zip"
echo "Creating Portal bundle zip from ${BUNDLE_DIR}…"
# Portal expects Maven repo layout at the zip root (group folders first).
( cd "$BUNDLE_DIR" && zip -r -q "$ZIP_PATH" . )
ls -lh "$ZIP_PATH"

echo "Uploading bundle to Central Publisher Portal (publishingType=AUTOMATIC)…"
UPLOAD_RESP="$(
  curl --silent --show-error --connect-timeout 30 --max-time 600 \
    --write-out $'\n%{http_code}' \
    -X POST \
    -H "Authorization: Bearer ${AUTH}" \
    -F "bundle=@${ZIP_PATH};type=application/octet-stream" \
    "${PORTAL_API}/upload?name=criollo-kmp-foundation-${RELEASE_VERSION}&publishingType=AUTOMATIC"
)"
HTTP_CODE="${UPLOAD_RESP##*$'\n'}"
BODY="${UPLOAD_RESP%$'\n'*}"
echo "upload HTTP ${HTTP_CODE}: ${BODY}"

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
  echo "::error title=Portal bundle upload failed (HTTP ${HTTP_CODE})::${BODY}"
  exit 1
fi

DEPLOYMENT_ID="$(printf '%s' "$BODY" | tr -d '[:space:]')"
if [ -z "$DEPLOYMENT_ID" ]; then
  echo "::error title=Missing Portal deployment id::Upload returned empty body"
  exit 1
fi
echo "Portal deployment id: ${DEPLOYMENT_ID}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "deployment_id=${DEPLOYMENT_ID}" >> "$GITHUB_OUTPUT"
fi

deadline=$((SECONDS + TIMEOUT_SECONDS))
published=0
while [ "$SECONDS" -lt "$deadline" ]; do
  STATUS_RESP="$(
    curl --silent --show-error --connect-timeout 30 --max-time 120 \
      --write-out $'\n%{http_code}' \
      -X POST \
      -H "Authorization: Bearer ${AUTH}" \
      -H "Accept: application/json" \
      "${PORTAL_API}/status?id=${DEPLOYMENT_ID}"
  )"
  HTTP_CODE="${STATUS_RESP##*$'\n'}"
  BODY="${STATUS_RESP%$'\n'*}"
  if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
    echo "status HTTP ${HTTP_CODE}: ${BODY}"
    sleep "$POLL_SECONDS"
    continue
  fi

  STATE="$(
    printf '%s' "$BODY" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("deploymentState") or d.get("state") or "")'
  )"
  echo "deploymentState=${STATE}"

  case "$STATE" in
    PUBLISHED)
      published=1
      break
      ;;
    FAILED)
      echo "::error title=Central Portal deployment FAILED::${BODY}"
      exit 1
      ;;
    PENDING|VALIDATING|VALIDATED|PUBLISHING|"")
      ;;
    *)
      echo "Unknown deploymentState=${STATE}; continuing to poll"
      ;;
  esac
  sleep "$POLL_SECONDS"
done

if [ "$published" -ne 1 ]; then
  echo "::error title=Portal publish timed out::deployment ${DEPLOYMENT_ID} did not reach PUBLISHED within ${TIMEOUT_SECONDS}s"
  exit 1
fi

echo "::notice title=Maven Central::Portal deployment ${DEPLOYMENT_ID} is PUBLISHED. Still verifying repo1 artifact set…"
