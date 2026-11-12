#!/usr/bin/env bash
# Report Codacy quality signals: issues, complexity, duplication (and optional PR delta).
#
# Auth (first match wins):
#   CODACY_API_TOKEN env, or local.properties keys codacyApiToken / codacyToken
#
# Usage:
#   ./scripts/codacy-report.sh
#   ./scripts/codacy-report.sh --pr 45
#   ./scripts/codacy-report.sh --reanalyze HEAD
#   ./scripts/codacy-report.sh --reanalyze <commitSha>
#
# Notes:
#   - File metrics (complexity/duplication) require sort=duplication|complexity on the files API.
#   - Cloud data is for the last analyzed commit on the selected branch (push + reanalyze to refresh).
#   - Local parity before push: ./gradlew localCloudParity qualityCheck
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROVIDER="${CODACY_PROVIDER:-gh}"
ORG="${CODACY_ORG:-jdbenitez94}"
REPO="${CODACY_REPO:-criollo-kmp-foundation}"
BRANCH=""
PR=""
TOP=25
REANALYZE_COMMIT=""

usage() {
  sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --provider) PROVIDER="$2"; shift 2 ;;
    --org) ORG="$2"; shift 2 ;;
    --repo) REPO="$2"; shift 2 ;;
    --branch) BRANCH="$2"; shift 2 ;;
    --pr) PR="$2"; shift 2 ;;
    --top) TOP="$2"; shift 2 ;;
    --reanalyze)
      shift
      if [[ $# -gt 0 && "$1" != --* ]]; then
        REANALYZE_COMMIT="$1"
        shift
      else
        REANALYZE_COMMIT="HEAD"
      fi
      ;;
    -h|--help) usage 0 ;;
    *)
      echo "Unknown arg: $1" >&2
      usage 1
      ;;
  esac
done

if [[ -n "$REANALYZE_COMMIT" ]]; then
  if [[ "$REANALYZE_COMMIT" == "HEAD" ]]; then
    REANALYZE_COMMIT="$(git -C "$ROOT" rev-parse HEAD)"
  fi
fi

resolve_token() {
  if [[ -n "${CODACY_API_TOKEN:-}" ]]; then
    printf '%s' "$CODACY_API_TOKEN"
    return
  fi
  local props="$ROOT/local.properties"
  if [[ -f "$props" ]]; then
    local line
    line="$(awk -F= '/^codacyApiToken=/{print substr($0,index($0,"=")+1); exit}' "$props")"
    if [[ -z "$line" ]]; then
      line="$(awk -F= '/^codacyToken=/{print substr($0,index($0,"=")+1); exit}' "$props")"
    fi
    if [[ -n "$line" ]]; then
      printf '%s' "$line"
      return
    fi
  fi
  echo "Missing Codacy token (CODACY_API_TOKEN or local.properties codacyApiToken)." >&2
  exit 1
}

TOKEN="$(resolve_token)"
export CODACY_TOKEN_FOR_REPORT="$TOKEN"
export CODACY_PROVIDER="$PROVIDER"
export CODACY_ORG="$ORG"
export CODACY_REPO="$REPO"
export CODACY_BRANCH="${BRANCH}"
export CODACY_PR="${PR}"
export CODACY_TOP="$TOP"
export CODACY_REANALYZE_COMMIT="${REANALYZE_COMMIT}"

python3 - <<'PY'
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

TOKEN = os.environ["CODACY_TOKEN_FOR_REPORT"]
PROVIDER = os.environ["CODACY_PROVIDER"]
ORG = os.environ["CODACY_ORG"]
REPO = os.environ["CODACY_REPO"]
BRANCH = os.environ.get("CODACY_BRANCH") or ""
PR = os.environ.get("CODACY_PR") or ""
TOP = int(os.environ.get("CODACY_TOP") or "25")
REANALYZE = os.environ.get("CODACY_REANALYZE_COMMIT") or ""

ORG_BASE = f"https://app.codacy.com/api/v3/organizations/{PROVIDER}/{ORG}/repositories/{REPO}"
ANAL_BASE = (
    f"https://api.codacy.com/api/v3/analysis/organizations/{PROVIDER}/{ORG}/repositories/{REPO}"
)


def request(
    method: str,
    url: str,
    body: dict[str, Any] | None = None,
    allow_empty: bool = False,
) -> Any:
    data = None
    headers = {
        "api-token": TOKEN,
        "Accept": "application/json",
    }
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return {} if allow_empty else {}
            return json.loads(raw)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")[:500]
        raise SystemExit(f"HTTP {e.code} {method} {url}\n{detail}") from e


if REANALYZE:
    print(f"Requesting Codacy reanalyze for commit {REANALYZE}…")
    request(
        "POST",
        f"{ORG_BASE}/reanalyzeCommit",
        {"commitUuid": REANALYZE},
        allow_empty=True,
    )
    print("Reanalyze accepted (HTTP 2xx). Metrics refresh asynchronously.")
    print()


def paginate_get(url: str, params: dict[str, Any] | None = None) -> list[Any]:
    params = dict(params or {})
    params.setdefault("limit", 100)
    out: list[Any] = []
    cursor = None
    while True:
        q = dict(params)
        if cursor:
            q["cursor"] = cursor
        full = f"{url}?{urllib.parse.urlencode(q)}"
        payload = request("GET", full)
        out.extend(payload.get("data") or [])
        cursor = (payload.get("pagination") or {}).get("cursor")
        if not cursor:
            break
    return out


def num(row: dict[str, Any], *keys: str) -> float:
    for key in keys:
        value = row.get(key)
        if value is not None:
            try:
                return float(value)
            except (TypeError, ValueError):
                continue
    return 0.0


def section(title: str) -> None:
    print()
    print(f"== {title} ==")


def print_rows(rows: list[tuple[Any, ...]], headers: tuple[str, ...]) -> None:
    if not rows:
        print("(none)")
        return
    widths = [len(h) for h in headers]
    str_rows: list[tuple[str, ...]] = []
    for row in rows:
        cells = tuple("" if c is None else str(c) for c in row)
        str_rows.append(cells)
        for i, cell in enumerate(cells):
            widths[i] = max(widths[i], len(cell))
    fmt = "  ".join(f"{{:{w}}}" for w in widths)
    print(fmt.format(*headers))
    print(fmt.format(*("-" * w for w in widths)))
    for cells in str_rows:
        print(fmt.format(*cells))


print(f"Codacy report  {PROVIDER}/{ORG}/{REPO}" + (f"  branch={BRANCH}" if BRANCH else ""))

# --- Issues overview + open issues ---
section("Issues overview")
overview = request("POST", f"{ANAL_BASE}/issues/overview", {})
counts = ((overview.get("data") or {}).get("counts") or {})
levels = counts.get("levels") or []
categories = counts.get("categories") or []
if levels or categories:
    if levels:
        print("by severity:", ", ".join(f"{x.get('name')}={x.get('total')}" for x in levels))
    if categories:
        print(
            "by category:",
            ", ".join(f"{x.get('name')}={x.get('total')}" for x in categories),
        )
else:
    print("no open-issue aggregates (empty overview)")

section(f"Open issues (top {TOP})")
search_body: dict[str, Any] = {}
if BRANCH:
    search_body["branchNames"] = [BRANCH]
issues_payload = request(
    "POST",
    f"{ANAL_BASE}/issues/search?{urllib.parse.urlencode({'limit': min(TOP, 100)})}",
    search_body,
)
issue_rows = []
for issue in issues_payload.get("data") or []:
    pattern = issue.get("patternInfo") or {}
    issue_rows.append(
        (
            issue.get("severity") or issue.get("level") or pattern.get("severity") or "",
            issue.get("toolInfo", {}).get("name")
            or (issue.get("tool") or {}).get("name")
            or "",
            issue.get("lineNumber") or issue.get("line") or "",
            issue.get("filePath")
            or (issue.get("file") or {}).get("path")
            or "",
            (issue.get("message") or pattern.get("title") or "")[:90],
        )
    )
print_rows(issue_rows[:TOP], ("sev", "tool", "line", "path", "message"))
total_issues = (issues_payload.get("pagination") or {}).get("total")
if total_issues is not None:
    print(f"total open issues: {total_issues}")

# --- File metrics (sort required for complexity/duplication fields) ---
section(f"Duplication (top {TOP})")
file_params: dict[str, Any] = {"limit": 100, "sort": "duplication", "direction": "desc"}
if BRANCH:
    file_params["branchName"] = BRANCH
files = paginate_get(f"{ORG_BASE}/files", file_params)
dup_rows = [
    (
        int(num(f, "duplication")),
        int(num(f, "numberOfClones")),
        f.get("gradeLetter") or "",
        f.get("path"),
    )
    for f in files
    if num(f, "duplication") > 0
]
dup_rows.sort(key=lambda r: (-r[0], -r[1], r[3] or ""))
print_rows(dup_rows[:TOP], ("dupLines", "clones", "grade", "path"))

section(f"Complexity (top {TOP})")
# Prefer complexity sort so the metric is populated; fall back to previous list.
cx_params = dict(file_params)
cx_params["sort"] = "complexity"
cx_files = paginate_get(f"{ORG_BASE}/files", cx_params)
cx_rows = [
    (
        int(num(f, "complexity")),
        int(num(f, "numberOfMethods")),
        f.get("gradeLetter") or "",
        f.get("path"),
    )
    for f in cx_files
]
cx_rows.sort(key=lambda r: (-r[0], r[3] or ""))
print_rows(cx_rows[:TOP], ("complexity", "methods", "grade", "path"))

section(f"Files with issues (top {TOP})")
issue_file_rows = [
    (int(num(f, "totalIssues")), f.get("gradeLetter") or "", f.get("path"))
    for f in cx_files
    if num(f, "totalIssues") > 0
]
issue_file_rows.sort(key=lambda r: (-r[0], r[2] or ""))
print_rows(issue_file_rows[:TOP], ("issues", "grade", "path"))

# --- Optional PR ---
if PR:
    section(f"Pull request #{PR}")
    pr = request("GET", f"{ANAL_BASE}/pull-requests/{PR}")
    info = pr.get("pullRequest") or {}
    quality = pr.get("quality") or {}
    coverage = pr.get("coverage") or {}
    print(f"title:        {info.get('title')}")
    print(f"branches:     {info.get('originBranch')} -> {info.get('targetBranch')}")
    print(f"head:         {info.get('headCommitSha')}")
    print(f"analysing:    {pr.get('isAnalysing')}")
    print(f"upToStandards:{pr.get('isUpToStandards')}")
    print(
        "quality:      "
        f"newIssues={quality.get('newIssues')} fixed={quality.get('fixedIssues')} "
        f"deltaComplexity={quality.get('deltaComplexity')} "
        f"deltaClones={quality.get('deltaClonesCount')}"
    )
    diff = (coverage.get("diffCoverage") or {})
    print(
        "coverage:     "
        f"delta={coverage.get('deltaCoverage')} "
        f"diffCovered={diff.get('coveredLines')}/{diff.get('coverableLines')} "
        f"({diff.get('cause') or 'ok'})"
    )

    section(f"PR #{PR} issues")
    pr_issues = paginate_get(f"{ANAL_BASE}/pull-requests/{PR}/issues", {"limit": 100})
    pr_issue_rows = []
    for issue in pr_issues:
        pattern = issue.get("patternInfo") or {}
        pr_issue_rows.append(
            (
                issue.get("severity") or issue.get("level") or "",
                issue.get("lineNumber") or issue.get("line") or "",
                issue.get("filePath")
                or (issue.get("file") or {}).get("path")
                or "",
                (issue.get("message") or pattern.get("title") or "")[:90],
            )
        )
    print_rows(pr_issue_rows[:TOP], ("sev", "line", "path", "message"))

    section(f"PR #{PR} changed files (quality delta)")
    pr_files = paginate_get(f"{ANAL_BASE}/pull-requests/{PR}/files", {"limit": 100})
    pr_file_rows = []
    for item in pr_files:
        path = (item.get("file") or {}).get("path")
        q = item.get("quality") or {}
        pr_file_rows.append(
            (
                q.get("deltaNewIssues") or 0,
                q.get("deltaFixedIssues") or 0,
                q.get("deltaComplexity") or 0,
                q.get("deltaClonesCount") or 0,
                path,
            )
        )
    pr_file_rows.sort(key=lambda r: (-abs(int(r[2])), -int(r[0]), r[4] or ""))
    print_rows(
        pr_file_rows[:TOP],
        ("newIss", "fixed", "dCx", "dClones", "path"),
    )

print()
print(
    "Tip: local loop before push → ./gradlew localCloudParity qualityCheck "
    "(jscpd + Detekt). Cloud metrics refresh after Codacy analyzes the commit."
)
PY
