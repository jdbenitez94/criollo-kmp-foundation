#!/usr/bin/env bash
# Close the OSSRH Staging API default repository and drive the Central Portal
# deployment to PUBLISHED (or fail). Success here is necessary but not sufficient —
# callers must also run verify-release-on-central.sh against repo1.
#
# Required env:
#   OSSRH_USERNAME, OSSRH_PASSWORD, MAVEN_CENTRAL_NAMESPACE
# Optional env:
#   PORTAL_POLL_SECONDS (default 20)
#   PORTAL_TIMEOUT_SECONDS (default 2400 = 40m)
set -euo pipefail

: "${OSSRH_USERNAME:?}"
: "${OSSRH_PASSWORD:?}"
: "${MAVEN_CENTRAL_NAMESPACE:?}"

POLL_SECONDS="${PORTAL_POLL_SECONDS:-20}"
TIMEOUT_SECONDS="${PORTAL_TIMEOUT_SECONDS:-2400}"
AUTH="$(printf '%s:%s' "$OSSRH_USERNAME" "$OSSRH_PASSWORD" | base64 | tr -d '\n')"
STAGING_API="https://ossrh-staging-api.central.sonatype.com"
PORTAL_API="https://central.sonatype.com/api/v1/publisher"

curl_auth() {
  local method="$1"
  local url="$2"
  shift 2
  curl --silent --show-error --connect-timeout 30 --max-time 300 \
    --write-out $'\n%{http_code}' \
    -X "$method" \
    -H "Authorization: Bearer ${AUTH}" \
    -H "Accept: application/json" \
    "$@" \
    "$url"
}

split_response() {
  local response
  response="$(cat)"
  HTTP_CODE="${response##*$'\n'}"
  BODY="${response%$'\n'*}"
}

extract_uuid() {
  python3 -c '
import json, re, sys
raw = sys.stdin.read().strip()
if not raw:
    raise SystemExit
if re.fullmatch(r"[0-9a-fA-F-]{36}", raw):
    print(raw)
    raise SystemExit
try:
    data = json.loads(raw)
except json.JSONDecodeError:
    m = re.search(
        r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
        raw,
    )
    if m:
        print(m.group(0))
    raise SystemExit
if isinstance(data, str) and re.fullmatch(r"[0-9a-fA-F-]{36}", data):
    print(data)
    raise SystemExit
if isinstance(data, dict):
    for key in ("portal_deployment_id", "deploymentId", "deployment_id", "id"):
        val = data.get(key)
        if isinstance(val, str) and val:
            print(val)
            raise SystemExit
    repos = data.get("repositories")
    if isinstance(repos, list):
        for repo in repos:
            if not isinstance(repo, dict):
                continue
            for key in ("portal_deployment_id", "deploymentId", "deployment_id"):
                val = repo.get(key)
                if isinstance(val, str) and val:
                    print(val)
                    raise SystemExit
'
}

echo "Listing open staging repositories (client IP) before finalize..."
SEARCH_RESP="$(curl_auth GET \
  "${STAGING_API}/manual/search/repositories?ip=client&state=open" || true)"
split_response <<<"$SEARCH_RESP" || true
echo "search HTTP ${HTTP_CODE:-?}: ${BODY:-<empty>}"

# portal_api: hand off to Portal and return a deployment id we can poll / publish.
# Do not trust a bare automatic 2xx — 0.1.6/0.1.7 both "succeeded" with incomplete sets.
# URL path matches the working CI finalize step (only publishing_type differs).
UPLOAD_URL="${STAGING_API}/manual/upload/defaultRepository/${MAVEN_CENTRAL_NAMESPACE}?publishing_type=portal_api"
echo "Finalizing default repository → Portal (publishing_type=portal_api)..."
UPLOAD_RESP="$(curl_auth POST "$UPLOAD_URL" -H "Content-Type: application/json")"
split_response <<<"$UPLOAD_RESP"
echo "finalize HTTP ${HTTP_CODE}: ${BODY}"

if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
  echo "::error title=Central Portal finalize failed (HTTP ${HTTP_CODE})::${BODY}"
  exit 1
fi

DEPLOYMENT_ID="$(printf '%s' "$BODY" | extract_uuid || true)"

if [ -z "${DEPLOYMENT_ID:-}" ]; then
  echo "No deployment id in finalize body; searching repositories..."
  SEARCH_RESP="$(curl_auth GET \
    "${STAGING_API}/manual/search/repositories?ip=client")"
  split_response <<<"$SEARCH_RESP"
  echo "search HTTP ${HTTP_CODE}: ${BODY}"
  DEPLOYMENT_ID="$(printf '%s' "$BODY" | extract_uuid || true)"
fi

if [ -z "${DEPLOYMENT_ID:-}" ]; then
  echo "::error title=Missing Portal deployment id::Finalize succeeded but no portal_deployment_id was returned. Check https://central.sonatype.com/publishing"
  exit 1
fi

echo "Portal deployment id: ${DEPLOYMENT_ID}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "deployment_id=${DEPLOYMENT_ID}" >> "$GITHUB_OUTPUT"
fi

deadline=$((SECONDS + TIMEOUT_SECONDS))
published=0

while [ "$SECONDS" -lt "$deadline" ]; do
  STATUS_RESP="$(curl_auth POST \
    "${PORTAL_API}/status?id=${DEPLOYMENT_ID}" \
    -H "Content-Type: application/json")"
  split_response <<<"$STATUS_RESP"
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
    VALIDATED)
      echo "Validated — requesting publish..."
      PUB_RESP="$(curl_auth POST "${PORTAL_API}/deployment/${DEPLOYMENT_ID}")"
      split_response <<<"$PUB_RESP"
      echo "publish HTTP ${HTTP_CODE}: ${BODY}"
      if [ "$HTTP_CODE" -lt 200 ] || [ "$HTTP_CODE" -ge 300 ]; then
        echo "::error title=Portal publish request failed (HTTP ${HTTP_CODE})::${BODY}"
        exit 1
      fi
      ;;
    PENDING|VALIDATING|PUBLISHING|"")
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
