# Kover coverage

Coverage is configured via Gradle convention plugins (Kover 0.9.x DSL), not a checked-in `kover.xml`.

## Plugins

| Plugin ID | Applied to | Role |
| --- | --- | --- |
| `convention.kover.library` | Published KMP libraries via `criollo.kmp-library` | Per-module reports |
| `convention.kover.aggregation` | Root (via `convention.root`) | Merges library modules and enforces verify rules |

Implementation: [`KoverLibraryConventionPlugin.kt`](../../build-logic/convention/src/main/kotlin/KoverLibraryConventionPlugin.kt), [`KoverAggregationConventionPlugin.kt`](../../build-logic/convention/src/main/kotlin/KoverAggregationConventionPlugin.kt).

## Thresholds

- **Published libraries** (`:coroutines`, `:coroutines:compose`, `:coroutines:viewmodel`): minimum **70%** line coverage (`published-library-line-coverage`).
- `:bom` and `:project-conventions` are outside the merged Kover gate (platform / Gradle plugin).

## Commands

```bash
./gradlew koverVerify          # fail if below threshold
./gradlew koverHtmlReport      # HTML report
./gradlew qualityCheck         # includes koverVerify (+ installGitHooks)
```
