# Commit conventions

This repo uses [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## Types

| Type | When |
|------|------|
| `feat` | New user-facing capability or published API |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `ci` | GitHub Actions / Dependabot |
| `build` | Gradle, build-logic, version catalog |
| `chore` | Maintenance that is not `feat`/`fix`/`docs`/`ci`/`build` |
| `refactor` | Internal change without API/behavior change |
| `test` | Tests only |
| `perf` | Performance improvement |

Breaking changes: append `!` after type/scope (`feat(coroutines)!: …`) and/or add a
`BREAKING CHANGE:` footer.

On `main`, [release-please](../.github/workflows/release-please.yml) turns these commits into
version bumps and `vX.Y.Z` tags (`feat` → minor while `<1.0.0`, `fix` → patch, `!` /
`BREAKING CHANGE` → major). `chore` / `ci` / `docs` alone do not cut a release.

## Scopes (optional)

Prefer a short module/area name when useful:

- `coroutines` (core / `compose` / `viewmodel` submodules)
- `bom`, `project-conventions`
- `build-logic`, `detekt`, `kover`

Examples:

```text
feat(coroutines): add Debounce policy overload
docs: clarify BOM install in README
chore(deps): bump GitHub Actions and Kover
ci: add docs link check workflow
```
