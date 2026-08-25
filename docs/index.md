# Criollo KMP Foundation

Kotlin Multiplatform building blocks for the Criollo architecture.

This release focuses on **keyed coroutine task management** (`TaskScope`): launch, skip,
replace, and debounce side effects by key.

**Group:** `io.github.jdbenitez94.criollo.kmp.foundation`

## Artifacts

| Artifact | Maven name | Gradle project | Role |
| ---------- | ------------ | ---------------- | ------ |
| BOM | `bom` | `:bom` | Aligns versions of all foundation modules |
| Core | `coroutines` | `:coroutines` | `TaskScope` registry |
| ViewModel | `coroutines-viewmodel` | `:coroutines:viewmodel` | `by taskScope()` on `ViewModel` |
| Compose | `coroutines-compose` | `:coroutines:compose` | `rememberTaskScope()` in Composables |
| Result | `result` | `:result` | Flow `Result` triad + `asResult()` |
| Runtime | `runtime` | `:runtime` | `retryWithBackoff` |
| Tooling | `project-conventions` | `:project-conventions` | Gradle plugin to sync shared style configs |
| Crypto | `kryptostore-crypto` | `:kryptostore:crypto` | Platform crypto for KryptoStore |
| Serializers | `kryptostore-serializers` | `:kryptostore:serializers` | Encrypted Okio serializers |
| Core | `kryptostore` | `:kryptostore` | Encrypted typed DataStore factories + IndexedDB |
| Preferences | `kryptostore-preferences` | `:kryptostore:preferences` | Encrypted + plain Preferences factories |
| Android DX | `kryptostore-android-delegates` | `:kryptostore:android` | Context property delegates |
| Migrate Android | `kryptostore-migrate-android` | `:kryptostore:migrate-android` | Unenveloped AEAD migration helpers |
| Testing | `testing` | `:testing` | JUnit 5 helpers (`MainTestDispatcherExtension`) |

## Guides

- [Managed tasks (`TaskScope`)](managed-tasks.md)
- [Flow Result](result.md)
- [Runtime retry](runtime.md)
- [Testing (JUnit 5)](testing.md)
- [Project conventions](project-conventions.md)
- [Publishing](publishing.md)
- [API reference (Dokka)](api.md)
- [KryptoStore crypto notes](kryptostore-crypto.md) — rotation (incl. Web multi-key), StreamingAead, iOS Keychain
- [KryptoStore migration](kryptostore-migration.md) — plaintext / AEAD / ESP / osipxd
- [KryptoStore completion (G1–G10)](kryptostore-complete.md)

## Guides (continued)

See also the [KryptoStore SDD+TDD spec](spec-final-kryptostore.md).

Source repository: [jdbenitez94/criollo-kmp-foundation](https://github.com/jdbenitez94/criollo-kmp-foundation).
