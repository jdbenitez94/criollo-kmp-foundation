#!/usr/bin/env bash
# Poll repo1.maven.org until every expected release artifact POM is HTTP 200.
#
# Required env:
#   RELEASE_VERSION
# Optional env:
#   GROUP_PATH (default io/github/jdbenitez94/criollo/kmp/foundation)
#   EXPECTED_ARTIFACTS_FILE (default beside this script)
#   VERIFY_POLL_SECONDS (default 30)
#   VERIFY_TIMEOUT_SECONDS (default 2400 = 40m)
#   REPO_BASE (default https://repo1.maven.org/maven2)
set -euo pipefail

: "${RELEASE_VERSION:?}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GROUP_PATH="${GROUP_PATH:-io/github/jdbenitez94/criollo/kmp/foundation}"
EXPECTED_ARTIFACTS_FILE="${EXPECTED_ARTIFACTS_FILE:-${SCRIPT_DIR}/expected-release-artifacts.txt}"
POLL_SECONDS="${VERIFY_POLL_SECONDS:-30}"
TIMEOUT_SECONDS="${VERIFY_TIMEOUT_SECONDS:-2400}"
REPO_BASE="${REPO_BASE:-https://repo1.maven.org/maven2}"

if [ ! -f "$EXPECTED_ARTIFACTS_FILE" ]; then
  echo "::error title=Missing expected artifacts list::${EXPECTED_ARTIFACTS_FILE}"
  exit 1
fi

EXPECTED_URLS=()
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    ''|\#*) continue ;;
  esac
  # shellcheck disable=SC2086
  set -- $line
  kind="$1"
  case "$kind" in
    root)
      art="$2"
      EXPECTED_URLS+=("${REPO_BASE}/${GROUP_PATH}/${art}/${RELEASE_VERSION}/${art}-${RELEASE_VERSION}.pom")
      ;;
    plugin)
      art="$2"
      EXPECTED_URLS+=("${REPO_BASE}/${GROUP_PATH}/${art}/${RELEASE_VERSION}/${art}-${RELEASE_VERSION}.pom")
      ;;
    platform)
      art="$2-$3"
      EXPECTED_URLS+=("${REPO_BASE}/${GROUP_PATH}/${art}/${RELEASE_VERSION}/${art}-${RELEASE_VERSION}.pom")
      ;;
    *)
      echo "::error title=Bad expected-artifacts line::${line}"
      exit 1
      ;;
  esac
done < "$EXPECTED_ARTIFACTS_FILE"

echo "Verifying ${#EXPECTED_URLS[@]} Maven Central POMs for ${RELEASE_VERSION}…"
deadline=$((SECONDS + TIMEOUT_SECONDS))

while true; do
  missing=()
  for url in "${EXPECTED_URLS[@]}"; do
    code="$(curl --silent --show-error --connect-timeout 15 --max-time 60 \
      --output /dev/null --write-out '%{http_code}' --head "$url" || true)"
    if [ "$code" != "200" ]; then
      missing+=("$code $url")
    fi
  done

  if [ "${#missing[@]}" -eq 0 ]; then
    echo "::notice title=Maven Central::All ${#EXPECTED_URLS[@]} expected POMs are present for ${RELEASE_VERSION}"
    exit 0
  fi

  if [ "$SECONDS" -ge "$deadline" ]; then
    echo "::error title=Incomplete Maven Central release::Timed out after ${TIMEOUT_SECONDS}s; still missing ${#missing[@]} / ${#EXPECTED_URLS[@]} POMs:"
    printf '  %s\n' "${missing[@]}"
    exit 1
  fi

  echo "Still missing ${#missing[@]} / ${#EXPECTED_URLS[@]} (retry in ${POLL_SECONDS}s). First gaps:"
  i=0
  for m in "${missing[@]}"; do
    echo "  $m"
    i=$((i + 1))
    [ "$i" -ge 8 ] && break
  done
  sleep "$POLL_SECONDS"
done
