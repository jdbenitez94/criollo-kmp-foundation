#!/usr/bin/env python3
"""Collapse 3+ consecutive newlines in CHANGELOG.md (release-please MD012)."""
from __future__ import annotations

import re
import sys
from pathlib import Path


def main() -> int:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "CHANGELOG.md")
    text = path.read_text(encoding="utf-8")
    fixed = re.sub(r"\n{3,}", "\n\n", text)
    if fixed == text:
        print(f"{path}: already normalized")
        return 0
    path.write_text(fixed, encoding="utf-8")
    print(f"{path}: collapsed consecutive blank lines")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
