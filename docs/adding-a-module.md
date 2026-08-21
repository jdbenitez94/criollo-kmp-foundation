# Adding a module

Checklist for a new KMP artifact in this monorepo.

Library adapters that belong to an existing family should nest under that project
(e.g. `:coroutines:compose`, `:coroutines:viewmodel`) while keeping hyphenated Maven artifact ids
via `canonicalArtifactId` in `CriolloPublishingCommon`.

For a **published Gradle plugin** (see `:project-conventions`), use `org.jetbrains.kotlin.jvm` +
`java-gradle-plugin` + `criollo.maven-publish` instead of `criollo.kmp-library`, and wire
`publishToMavenLocal` / tests in [`RootPlugin`](../build-logic/convention/src/main/kotlin/RootPlugin.kt).

1. **Create the project directory** (e.g. `logging/`) with `build.gradle.kts` applying:
   - `criollo.kmp-library` (applies Kotlin Multiplatform + Android KMP library + quality/publish conventions)
   - Extra plugins only if needed (Compose, serialization, …) — apply `criollo.kmp-library` first

2. **Register in** [`settings.gradle.kts`](../settings.gradle.kts):

   ```kotlin
   include(":logging")
   ```

3. **Extend** [`ProjectConfig`](../build-logic/convention/src/main/kotlin/io/github/jdbenitez94/criollo/kmp/foundation/buildlogic/ProjectConfig.kt):
   - `Artifacts.*` constant
   - `Namespaces.*` Android namespace
   - POM description branch in `CriolloPublishingCommon.kt`
   - Add an `api(project(":…"))` constraint in [`bom/build.gradle.kts`](../bom/build.gradle.kts)

4. **Teach** [`CriolloKmpLibraryConventionPlugin`](../build-logic/convention/src/main/kotlin/CriolloKmpLibraryConventionPlugin.kt) the new `:path` → namespace mapping.

5. **Wire verification** in [`RootPlugin`](../build-logic/convention/src/main/kotlin/RootPlugin.kt) (`jvmLibraryTests` dependsOn).

6. **JS/Wasm:** if the module uses browser targets, consider copying `webpack.config.d/resolve-fallback.js` — see [js-wasm.md](js-wasm.md).

7. **Document** the artifact in the root README and bump `CHANGELOG.md`.

8. **Consumers**: add Maven coordinates to their version catalog; keep `includeBuild` dependencySubstitution entries in sync for local dogfood.

9. **Quality**: run `./gradlew qualityCheck jvmLibraryTests` (installs git hooks via `installGitHooks`). See [contributing.md](contributing.md).
