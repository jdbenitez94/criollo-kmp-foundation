# Contributing

## Local Git hooks

Repository hooks are installed when Gradle configures verification (via `installGitHooks`,
a dependency of `qualityCheck`). You can also run it manually:

```bash
./gradlew installGitHooks
```

This sets `core.hooksPath` to [`gradle/hooks/`](../gradle/hooks):

| Hook | What it runs |
|------|----------------|
| `pre-commit` | `ktlintFormat` (re-stages Kotlin files), `ktlintCheck`, `detekt` (non-blocking) |
| `pre-push` | `qualityCheck jvmLibraryTests` (configuration cache warn) |

Logs: `build/hooks/logs/`.

## Quality gate

```bash
./gradlew qualityCheck jvmLibraryTests
```

## Docs site (local)

```bash
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements-docs.txt
mkdocs serve
./gradlew dokkaGenerateHtml
```

See also [commit conventions](commit-conventions.md) and [adding a module](adding-a-module.md).
