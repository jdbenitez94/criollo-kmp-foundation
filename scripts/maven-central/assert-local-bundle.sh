#!/usr/bin/env bash
# Fail fast if the local Central bundle directory is missing any expected POM.
# Run after: ./gradlew publishAllPublicationsToCentralBundleRepository
#
# Required env:
#   RELEASE_VERSION
# Optional env:
#   BUNDLE_DIR (default $PWD/build/central-bundle)
#   GROUP_PATH (default io/github/jdbenitez94/criollo/kmp/foundation)
#   EXPECTED_ARTIFACTS_FILE (default beside this script)
set -euo pipefail

: "${RELEASE_VERSION:?}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUNDLE_DIR="${BUNDLE_DIR:-$PWD/build/central-bundle}"
GROUP_PATH="${GROUP_PATH:-io/github/jdbenitez94/criollo/kmp/foundation}"
EXPECTED_ARTIFACTS_FILE="${EXPECTED_ARTIFACTS_FILE:-${SCRIPT_DIR}/expected-release-artifacts.txt}"

if [ ! -d "$BUNDLE_DIR" ]; then
  echo "::error title=Missing local bundle dir::${BUNDLE_DIR}"
  exit 1
fi
if [ ! -f "$EXPECTED_ARTIFACTS_FILE" ]; then
  echo "::error title=Missing expected artifacts list::${EXPECTED_ARTIFACTS_FILE}"
  exit 1
fi

missing=()
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    ''|\#*) continue ;;
  esac
  # shellcheck disable=SC2086
  set -- $line
  kind="$1"
  case "$kind" in
    root|plugin)
      art="$2"
      ;;
    platform)
      art="$2-$3"
      ;;
    *)
      echo "::error title=Bad expected-artifacts line::${line}"
      exit 1
      ;;
  esac
  pom="${BUNDLE_DIR}/${GROUP_PATH}/${art}/${RELEASE_VERSION}/${art}-${RELEASE_VERSION}.pom"
  if [ ! -f "$pom" ]; then
    missing+=("$pom")
  fi
done < "$EXPECTED_ARTIFACTS_FILE"

if [ "${#missing[@]}" -ne 0 ]; then
  echo "::error title=Incomplete local Central bundle::Missing ${#missing[@]} expected POM(s) under ${BUNDLE_DIR}:"
  printf '  %s\n' "${missing[@]}"
  exit 1
fi

echo "::notice title=Local Central bundle::All expected POMs present under ${BUNDLE_DIR} for ${RELEASE_VERSION}"
