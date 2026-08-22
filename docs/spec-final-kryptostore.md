# Spec Final — KryptoStore

**Document ID:** `spec-final-kryptostore`  
**Status:** AUTHORITATIVE for implementation  
**Library product name:** KryptoStore  
**Date:** 2026-08-22 (amended same day: home repo = criollo-kmp-foundation)  
**Authority order:** this file **overrides** `encrypted-datastore-lib.md`, `encrypted-datastore-conversation-handoff.md`, and stale notes in `composeApp/docs/CRYPTO_KMP.md` wherever they conflict.

**Home repository (normative):**

| Item | Value |
|------|--------|
| Local path | `/Users/jbenitez/Projects/criollo-kmp-foundation` |
| GitHub | https://github.com/jdbenitez94/criollo-kmp-foundation |
| Note | There is **no** `criollo-kmp-infrastructure` folder under Projects; the owner’s “infrastructure” monorepo is **`criollo-kmp-foundation`** (README: more infrastructure modules land here over time). |
| Existing artifacts | `bom`, `coroutines`, `coroutines-viewmodel`, `coroutines-compose`, `project-conventions` |
| Maven group (repo) | `io.github.jdbenitez94.criollo.kmp.foundation` |
| Module guide | `criollo-kmp-foundation/docs/adding-a-module.md` |
| Publish guide | `criollo-kmp-foundation/docs/publishing.md` |

**Source extraction (dogfood / migrate from):**

| Item | Value |
|------|--------|
| Sample / donor repo | `/Users/jbenitez/Projects/saveable` (`composeApp/core/crypto`, `composeApp/core/datastore`, settings wiring) |
| This spec file lives in | **`criollo-kmp-foundation/docs/spec-final-kryptostore.md`** (authoritative). Saveable keeps a short pointer at `saveable/docs/spec-final-kryptostore.md`. |

**Related (historical / supporting — in saveable):**

- `saveable/docs/encrypted-datastore-lib.md` — early design (superseded)
- `saveable/docs/encrypted-datastore-conversation-handoff.md` — conversation dump / rationale
- `saveable/composeApp/docs/CRYPTO_KMP.md` — sample crypto notes (**must be updated** when saveable consumes kryptostore; contains known inaccuracies)
- Reference tree: `saveable/local-extra/encrypted-datastore-1.1.1-beta03/`

**In-repo guides (foundation):**

- [adding-a-module.md](adding-a-module.md)
- [publishing.md](publishing.md)
- [contributing.md](contributing.md)

**Methodology:** Spec-Driven Development (SDD) + Test-Driven Development (TDD).  
No production code for a requirement lands without: (1) a REQ id in this spec, (2) failing tests first where practical, (3) acceptance criteria checked.

---

## 0. How an implementing agent must work

1. Read this entire document before coding.
2. Treat §2 Decision Register as **closed** unless the owner explicitly reopens a `DEC-*` id.
3. Work **phase by phase**. Do not skip acceptance gates.
4. For every `REQ-*`: write or extend tests listed in §11 **before** or with the implementation (TDD). Prefer red → green → refactor.
5. Do not add `androidx.security:security-crypto` or `androidx.datastore:datastore-tink` to any KryptoStore common/public classpath.
6. Conventional Commits; no Co-authored-by / AI attribution (`AGENTS.md`).
7. No git commit/push unless the owner asks.
8. After each phase: update the checklist in §13 and leave a short phase report (files touched, tests run, residual risks).

---

## 1. Vision & product statement

### 1.1 One-liner

**KryptoStore** is a Kotlin Multiplatform library that encrypts Jetpack DataStore payloads with platform-native key storage (Tink + Android Keystore, Tink on JVM, AES-GCM on iOS, WebCrypto on JS/Wasm), exposing an osipxd-like API without depending on deprecated Jetpack Security Crypto.

### 1.2 Tagline (README)

> DataStore + Tink / Keystore / WebCrypto — extended to iOS and Web with one Okio encryption contract.

### 1.3 Goals (complete product, not MVP-only)

| ID | Goal |
|----|------|
| G1 | Publishable multi-artifact KMP library usable without copying composeApp code |
| G2 | Targets: Android, JVM, iOS, JS, Wasm |
| G3 | Typed (proto/kotlinx) encrypted DataStore + encrypted Preferences + **plain** Preferences |
| G4 | Web: proto → IndexedDB; prefs → localStorage; crypto → WebCrypto; keys in IndexedDB (never localStorage) |
| G5 | Fail-closed by default; opt-in plaintext migration reads |
| G6 | Key rotation with **re-encryption** of registered stores (not “drop old data” as the only story) |
| G7 | Android DX delegates; BOM; binary compatibility validation; frozen blob compat tests |
| G8 | Migration guides/adapters from plaintext, osipxd, EncryptedSharedPreferences / AeadSerializer (Android) |
| G9 | Sample app (composeApp) becomes a thin consumer; remember-email demo uses **plain** prefs |
| G10 | Docs: README, CRYPTO, MIGRATION, CHANGELOG — accurate vs code |

### 1.4 Non-goals (still explicit)

| ID | Non-goal |
|----|----------|
| NG1 | Portable ciphertext / keys across OS families |
| NG2 | Replacing Jetpack DataStore |
| NG3 | Using `security-crypto` or `EncryptedFile` as a backend |
| NG4 | Making `datastore-tink` the KMP encryption path |
| NG5 | Silent rewrite-to-default on corruption (Google’s convenience default) as **library** default |

---

## 2. Decision Register (CLOSED)

Every formerly “open” question is decided here. Changing one requires an explicit owner amendment to this file.

| ID | Decision | Choice | Rationale |
|----|----------|--------|-----------|
| DEC-01 | Product / artifact name | **KryptoStore** / Maven artifact ids `kryptostore`, `kryptostore-crypto`, … | Owner filename; family of artifacts under foundation |
| DEC-02 | Maven `groupId` | **`io.github.jdbenitez94.criollo.kmp.foundation`** (same as coroutines/bom) | Matches foundation publishing; one BOM aligns all infrastructure modules |
| DEC-03 | Monorepo / path | **`criollo-kmp-foundation`**, projects under `kryptostore/` (e.g. `:kryptostore`, `:kryptostore:crypto`) | Owner: library is part of Criollo KMP infrastructure monorepo; follow `docs/adding-a-module.md` |
| DEC-04 | Package root | `io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.*` | Same pattern as `…foundation.coroutines` |
| DEC-05 | Wire magic | Keep **`SVBLENC1`** | Compatibility with existing sample blobs; document historical name; do not rename without envelope v2 |
| DEC-06 | Envelope version | `1` only for v1.x writes | Unknown versions → CorruptionException |
| DEC-07 | `allowPlaintextRead` | **Ship in v1**, default **`false`** | Fail-closed default; opt-in migrators |
| DEC-08 | Corruption default | **Fail-closed** + quarantine `*.corrupt` where filesystem exists | Match current composeApp serializers/handler |
| DEC-09 | Storage on Android prefs | **OkioStorage everywhere** for uniformity | Document Google FileStorage tradeoff; no split in v1 |
| DEC-10 | Web proto storage | **IndexedDB** (`IndexedDbStorage`) | Owner hard constraint; not OPFS for v1 |
| DEC-11 | Web prefs storage | **`WebLocalStorage`** (localStorage) | Owner hard constraint |
| DEC-12 | Web key storage | IndexedDB DB (current sample: `app-crypto`) via WebCrypto worker | CRYPTO_KMP.md “keys in localStorage” is **wrong** — fix docs |
| DEC-13 | Serializer pipeline | **OkioSerializer only** in core | Required for IndexedDB / WebLocalStorage |
| DEC-14 | `datastore-tink` | Not a dependency; document as Android-only alternative | Wrong IO contract for KMP |
| DEC-15 | `security-crypto` / osipxd runtime | Forbidden | Deprecated / non-KMP |
| DEC-16 | Rotation | Keep platform rotators **and** implement **store re-encrypt** API | “Complete” product; sample previously dropped data — KryptoStore must do better |
| DEC-17 | Streaming AEAD | Optional Android/JVM `Cipher` / serializer path in **complete** scope (Phase E) | Large payloads; not required for small settings |
| DEC-18 | iOS Keychain | Promote master keys to Keychain as part of complete scope (Phase E) | CRYPTO_KMP already flags sandbox-only as interim |
| DEC-19 | Web key rotation | Document limitation in v1.0; design interface for future multi-key WebCrypto | Non-extractable keys make classic rotation hard; do not fake it |
| DEC-20 | DataStore version | Align with repo catalog (`1.3.0-alpha09` today) in monorepo; **first Maven 1.0** prefers **stable** DataStore if available, else document alpha clearly | Risk management |
| DEC-21 | Logging coupling | No hard dep on composeApp logging/utils; `KryptoLogger` no-op + optional inject | Publishable artifact |
| DEC-22 | Remember-email | Sample feature **separate** from lib core; **must** use plain prefs API | Demonstrates plain tier |
| DEC-23 | Plain prefs naming | `createPlainPreferencesDataStore` / `plainPreferencesDataStore` — **never** `legacy*` in public API | Owner feedback |
| DEC-24 | Binary compatibility | Enable kotlinx binary-compatibility-validator on JVM public APIs from first publishable API freeze | osipxd-quality bar |
| DEC-25 | Migration adapters | Optional artifact `kryptostore-migrate-android` for osipxd / AeadSerializer / ESP → KryptoStore | Complete offering; not on common classpath |
| DEC-26 | Default AAD | `deriveStoreAssociatedData(storeName, schemaVersion)` = `"$storeName\|v$schemaVersion"` UTF-8 | Current code |
| DEC-27 | Singleton rule | Document + enforce via factories where possible: one DataStore instance per storage identity | DataStore contract |
| DEC-28 | Init gate | `CryptoRuntime.initialize()` → Ready before opening encrypted stores | Current CryptoManager pattern |
| DEC-29 | Sample extraction | **saveable** composeApp consumes foundation via `includeBuild` / Maven; delete duplicated crypto/datastore serializers from composeApp once published locally | Thin consumer; dogfood like TaskScope |
| DEC-30 | Test order | TDD per REQ; frozen blob fixtures before declaring format stable | SDD+TDD |
| DEC-31 | Build conventions | Use foundation `criollo.kmp-library`, `ProjectConfig`, RootPlugin, qualityCheck, release-please | Do not invent a parallel publish stack inside saveable |
| DEC-32 | BOM | Extend existing `:bom` with kryptostore constraints (do **not** create a second BOM artifact unless owner reopens) | One foundation BOM |
| DEC-33 | Working tree for implementation | Primary PRs land in **criollo-kmp-foundation**; saveable PRs only for consumer migration + CRYPTO_KMP hygiene | Clear ownership |

---

## 3. Architecture

### 3.1 Layering

```text
┌─────────────────────────────────────────────────────────────┐
│ App / Sample (composeApp) — Koin, paths, domain models      │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│ kryptostore-android — Context property delegates            │
└───────────────────────────┬─────────────────────────────────┘
┌───────────────────────────▼─────────────────────────────────┐
│ kryptostore-preferences — encrypted + plain Preferences API │
└───────────────────────────┬─────────────────────────────────┘
┌───────────────────────────▼─────────────────────────────────┐
│ kryptostore — typed factories + Storage (Okio / IndexedDB)  │
└───────────────────────────┬─────────────────────────────────┘
┌───────────────────────────▼─────────────────────────────────┐
│ kryptostore-serializers — envelope + Encrypted* Okio wraps  │
└───────────────────────────┬─────────────────────────────────┘
┌───────────────────────────▼─────────────────────────────────┐
│ kryptostore-crypto — Cipher, CryptoRuntime, KeyRotator      │
└─────────────────────────────────────────────────────────────┘

Optional: kryptostore-migrate-android
Existing: foundation :bom (extended)
```

### 3.2 Gradle modules (Maven artifacts)

All modules live in **`criollo-kmp-foundation`**. Hyphenated Maven ids via existing `canonicalArtifactId` / nested projects (same pattern as `:coroutines:compose` → `coroutines-compose`).

| Project path | Artifact id | Targets |
|--------------|-------------|---------|
| `:kryptostore:crypto` | `kryptostore-crypto` | KMP all |
| `:kryptostore:serializers` | `kryptostore-serializers` | KMP all |
| `:kryptostore` (or `:kryptostore:core`) | `kryptostore` | KMP all |
| `:kryptostore:preferences` | `kryptostore-preferences` | KMP all |
| `:kryptostore:android` | `kryptostore-android` | Android |
| `:kryptostore:migrate-android` | `kryptostore-migrate-android` | Android |
| `:bom` (existing) | `bom` | BOM — **add** kryptostore constraints |

**Preferred nesting** (mirror coroutines family):

```text
criollo-kmp-foundation/
  kryptostore/                 → artifact kryptostore (core factories)  OR empty umbrella
    crypto/
    serializers/
    preferences/
    android/
    migrate-android/
  bom/                         → existing; extend
```

Exact nesting (`:kryptostore` as umbrella vs leaf) must follow `adding-a-module.md` and `canonicalArtifactId` so Maven names stay `kryptostore-*`. Agent must verify naming with one `publishToMavenLocal` dry-run in Phase A.

**GroupId for all:** `io.github.jdbenitez94.criollo.kmp.foundation`  
**Packages:** `io.github.jdbenitez94.criollo.kmp.foundation.kryptostore…`

### 3.3 Allowed / forbidden dependencies

**commonMain allowed:**

- `androidx.datastore:datastore-core-okio`
- `androidx.datastore:datastore-preferences-core`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- `org.jetbrains.kotlinx:kotlinx-serialization-protobuf` (serializers)
- `com.squareup.okio:okio` (transitive via datastore-okio)

**Platform crypto:**

| Source set | Deps |
|------------|------|
| androidMain | `tink-android` |
| jvmMain | `tink` (+ JNA only if still required for sealed master key file) |
| iosMain | `dev.whyoleg.cryptography` core + apple provider |
| js/wasm | WebCrypto worker TS resources; **no** Tink |

**Forbidden on any kryptostore module classpath:**

- `androidx.security:security-crypto` (+ ktx)
- `androidx.datastore:datastore-tink`
- osipxd artifacts
- composeApp modules as `api` of published kryptostore artifacts

### 3.4 Extraction map (from saveable sample → foundation)

| Source (saveable today) | Destination (criollo-kmp-foundation) |
|-------------------------|--------------------------------------|
| `composeApp/core/crypto/**` | `:kryptostore:crypto` |
| Serializers + fail-closed in `composeApp/core/datastore` | `:kryptostore:serializers` |
| `IndexedDbStorage*`, FileSystem expect/actual, typed factories | `:kryptostore` (core artifact) |
| Preferences factories / plain prefs | `:kryptostore:preferences` |
| New Context delegates | `:kryptostore:android` |
| `composeApp/data/settings` Koin + `AppSettings` | **stays in saveable** (consumer) |
| This spec | Copy to `criollo-kmp-foundation/docs/spec-final-kryptostore.md`; saveable keeps a short pointer |

**Consumer wiring (saveable):**

1. During development: `includeBuild("../criollo-kmp-foundation")` + dependencySubstitution (same pattern used for foundation coroutines / TaskScope if present).
2. After Maven Central: version catalog + foundation `bom`.

**Version alignment:** saveable pins DataStore in its catalog (`1.3.0-alpha09` today). Foundation `libs.versions.toml` / BOM must pin a tested DataStore version for kryptostore; prefer stable for Maven 1.0 line (DEC-20).

---

## 4. Wire format (normative)

### 4.1 Layout

```text
offset 0:  magic UTF-8 "SVBLENC1"   (8 bytes)
offset 8:  envelopeVersion UInt8    (must be 1 for writers in 1.x)
offset 9:  ciphertext…              (AEAD output for inner plaintext)
```

Constant: `ENCRYPTED_BLOB_MAGIC = "SVBLENC1"`.

### 4.2 Inner plaintext

| Store kind | Inner bytes |
|------------|-------------|
| Typed | kotlinx.serialization protobuf encoding of `T` |
| Preferences | AndroidX `PreferencesSerializer` Okio encoding |

### 4.3 Associated data (AAD)

- Primary: `deriveStoreAssociatedData(storeName, schemaVersion)` → `"$storeName|v$schemaVersion".encodeToByteArray()`
- Optional `legacyAssociatedData`: tried after primary on decrypt only
- New writes always use primary AAD

### 4.4 Read algorithm (Encrypted serializers)

```text
IF empty → return defaultValue
IF NOT startsWith(magic):
  IF options.allowPlaintextRead → decode inner as plaintext
  ELSE → CorruptionException (rejectPlaintextPayload)
IF envelopeVersion != 1 → CorruptionException
ciphertext = rest
plain = decrypt(primaryAAD) OR decrypt(legacyAAD) OR fail
  CancellationException must propagate (never wrap as CorruptionException)
return decode(plain)
```

### 4.5 Write algorithm

```text
plain = encode(value)
ciphertext = cipher.encrypt(plain, primaryAAD)
write magic + version(1) + ciphertext
```

---

## 5. Public API (normative sketch)

Packages under `io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.*`.

### 5.1 Crypto — `kryptostore-crypto`

```kotlin
interface Cipher {
    suspend fun encrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray
    suspend fun decrypt(message: ByteArray, associatedData: ByteArray? = null): ByteArray
}

interface KeyRotator {
    /** @return true if key material changed and store re-encryption should run */
    suspend fun rotateKeyIfNeeded(): Boolean
}

sealed interface CryptoRuntimeState {
    data object Initializing : CryptoRuntimeState
    data object Ready : CryptoRuntimeState
    data class Error(val cause: Throwable) : CryptoRuntimeState
}

class CryptoRuntime(
    // platform stack factory
) {
    val state: StateFlow<CryptoRuntimeState>
    val cipher: Cipher // only valid when Ready — or expose via Ready handle
    suspend fun initialize()
}

/** Platform entry: Android needs Application Context registration. */
expect fun createPlatformCryptoStack(appId: String): PlatformCryptoStack

data class KeyRotationConfig(
    val rotationPeriod: Duration = 90.days,
    val initialBackoff: Duration = 500.milliseconds,
    val maxRetries: Int = 3,
)
```

**REQ notes:** Rename from `CryptoManager` → `CryptoRuntime` in public API (avoid Android “Manager” stigma); keep sample adapter if needed.

**Logging:** `fun interface KryptoLog { fun error(t: Throwable?, msg: () -> String) }` default no-op.

### 5.2 Options — `kryptostore-serializers`

```kotlin
class EncryptedStoreOptions {
    var storeName: String = "default"
    var schemaVersion: Int = 1
    var associatedData: ByteArray? = null // default derived from storeName|vN
    var legacyAssociatedData: ByteArray? = null
    var allowPlaintextRead: Boolean = false
}

fun deriveStoreAssociatedData(storeName: String, schemaVersion: Int): ByteArray
```

### 5.3 Serializers

```kotlin
class ProtoOkioSerializer<T : Any>(
    kSerializer: KSerializer<T>,
    override val defaultValue: T,
) : OkioSerializer<T>

class EncryptedProtoSerializer<T : Any>(
    inner: ProtoOkioSerializer<T>,
    cipher: Cipher,
    options: EncryptedStoreOptions,
) : OkioSerializer<T>

class EncryptedPreferencesSerializer(
    cipher: Cipher,
    options: EncryptedStoreOptions,
) : OkioSerializer<Preferences>
```

(Name `EncryptedOkioSerializer` may remain as typealias to `EncryptedProtoSerializer` during migration.)

### 5.4 Corruption

```kotlin
fun <T> failClosedCorruptionHandler(
    producePath: () -> Path,
    fileSystem: () -> FileSystem? = { kryptostoreFileSystem },
): ReplaceFileCorruptionHandler<T>
```

Web/IndexedDB: define equivalent “quarantine” strategy (rename key / delete + surface error) — must not silently serve defaults.

### 5.5 Core factories — `kryptostore`

```kotlin
fun <T : Any> createEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    storage: Storage<T>, // or overload with producePath for file targets
    migrations: List<DataMigration<T>> = emptyList(),
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null, // null → fail-closed when path known
): DataStore<T>

/** File targets helper */
fun <T : Any> createEncryptedProtoDataStore(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    producePath: () -> Path,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T>

/** Web typed helper — IndexedDB */
fun <T : Any> createEncryptedProtoDataStoreIndexedDb(
    cipher: Cipher,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    name: String,
    options: EncryptedStoreOptions = EncryptedStoreOptions(storeName = name),
    migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T>
```

### 5.6 Preferences — `kryptostore-preferences`

```kotlin
fun createEncryptedPreferencesDataStore(
    cipher: Cipher,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    storage: Storage<Preferences>,
    migrations: List<DataMigration<Preferences>> = emptyList(),
): DataStore<Preferences>

fun createEncryptedPreferencesDataStore(
    cipher: Cipher,
    producePath: () -> Path,
    options: EncryptedStoreOptions = EncryptedStoreOptions(),
    migrations: List<DataMigration<Preferences>> = emptyList(),
): DataStore<Preferences>

fun createEncryptedPreferencesDataStoreLocalStorage(
    cipher: Cipher,
    name: String,
    options: EncryptedStoreOptions = EncryptedStoreOptions(storeName = name),
): DataStore<Preferences>

fun createPlainPreferencesDataStore(
    producePath: () -> Path,
): DataStore<Preferences>

fun createPlainPreferencesDataStoreLocalStorage(
    name: String,
): DataStore<Preferences>
```

### 5.7 Android — `kryptostore-android`

```kotlin
fun <T : Any> Context.encryptedProtoDataStore(
    fileName: String,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    cipher: () -> Cipher,
    options: EncryptedStoreOptions.() -> Unit = {},
    // corruption / migrations / scope params aligned with DataStore delegates
): ReadOnlyProperty<Context, DataStore<T>>

fun Context.encryptedPreferencesDataStore(
    name: String,
    cipher: () -> Cipher,
    options: EncryptedStoreOptions.() -> Unit = {},
): ReadOnlyProperty<Context, DataStore<Preferences>>

fun Context.plainPreferencesDataStore(
    name: String,
): ReadOnlyProperty<Context, DataStore<Preferences>>
```

**Never** construct `EncryptedFile` / `MasterKeys`.

### 5.8 Re-encryption on rotation — `kryptostore` / crypto

```kotlin
interface EncryptedStoreHandle {
    suspend fun reEncryptInPlace()
}

class StoreRegistry {
    fun register(handle: EncryptedStoreHandle)
    suspend fun reEncryptAll()
}

// CryptoRuntime.initialize():
//   if (keyRotator.rotateKeyIfNeeded()) storeRegistry.reEncryptAll()
```

Semantics:

- After successful key rotation, every registered encrypted store must be readable with the new key material.
- If re-encrypt fails, surface `CryptoRuntimeState.Error`; do not claim Ready with mixed old/new ciphertext unless documented recovery mode exists.
- Web: rotation remains no-op until a real multi-key scheme exists; registry still works for AAD/schema migrations.

### 5.9 Migrate Android (optional artifact)

Provide documented one-shot migrations:

- Read osipxd / StreamingAead / Aead-shaped blobs where feasible with **Tink only** (no security-crypto dependency).
- Read `datastore-tink` `AeadSerializer` blobs.
- Migrate EncryptedSharedPreferences → Preferences DataStore (plain or encrypted).

If a format cannot be read without security-crypto, document destructive migration path.

---

## 6. Platform storage matrix (normative)

| Target | Typed encrypted | Prefs encrypted | Prefs plain | Key material |
|--------|-----------------|-----------------|-------------|--------------|
| Android | Okio file | Okio `*.preferences_pb` | Okio / `PreferenceDataStoreFactory.createWithPath` | Tink keyset + Android Keystore |
| JVM | Okio file | Okio | Okio / createWithPath | Tink + sealed local master |
| iOS | Okio file | Okio | Okio / createWithPath | AES-GCM; Keychain for master in complete scope |
| JS/Wasm | **IndexedDbStorage** | **WebLocalStorage** | WebLocalStorage + PreferencesSerializer | WebCrypto worker; keys in IndexedDB (`app-crypto` or configurable name) |

---

## 7. Requirements catalog (SDD)

Each requirement has ID, statement, acceptance criteria (AC), and primary tests (T).

### 7.1 Product & packaging

| ID | Requirement | Acceptance criteria | Tests |
|----|-------------|---------------------|-------|
| REQ-PKG-01 | `kryptostore/` modules exist in **criollo-kmp-foundation** and are included from its `settings.gradle.kts` per `adding-a-module.md` | `./gradlew :kryptostore:crypto:compileKotlinJvm` (from foundation root) succeeds | Build smoke |
| REQ-PKG-02 | Maven coordinates use foundation `group` + `kryptostore-*` artifact ids (DEC-01/02) | `publishToMavenLocal` dry-run shows correct coords | Publish dry-run |
| REQ-PKG-03 | Existing foundation `:bom` gains kryptostore (+ DataStore / Tink) constraints | Importing foundation BOM resolves kryptostore modules | BOM resolution test |
| REQ-PKG-04 | No forbidden deps on kryptostore classpaths | DependencyInsight / check fails if security-crypto or datastore-tink appear | ArchUnit or Gradle check |
| REQ-PKG-05 | No `api` dependency from kryptostore → saveable/composeApp | Same | Arch / dependency check |
| REQ-PKG-06 | Packages are `io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.*` | No public `composeApp.core.crypto` types remain as the **library** API | Compile consumers |
| REQ-PKG-07 | Module registration updates `ProjectConfig`, RootPlugin, namespaces, README, CHANGELOG in foundation | Checklist in `adding-a-module.md` complete | Review checklist |

### 7.2 Crypto

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-CRY-01 | `Cipher` encrypt/decrypt round-trip with AAD on every target | Round-trip equality; wrong AAD fails | jvmTest, androidHostTest, iosSimulator if CI, jsTest |
| REQ-CRY-02 | `CryptoRuntime.initialize()` is idempotent and mutex-safe | Concurrent initialize → single Ready | jvmTest |
| REQ-CRY-03 | Initialize failure → `Error` state, not Ready | State machine | jvmTest |
| REQ-CRY-04 | Android uses Keystore-backed master + Tink keyset | Documented; integration test on device/host | androidHostTest |
| REQ-CRY-05 | JVM master key file is restricted permissions where OS allows | Existing sealing behavior preserved or improved | jvmTest |
| REQ-CRY-06 | iOS AES-GCM via whyoleg works | Round-trip | ios tests when Xcode available |
| REQ-CRY-07 | Web uses WebCrypto worker; keys in IndexedDB; **not** localStorage | Inspect worker source + unit tests of bindings | jsTest; static assert worker DB name |
| REQ-CRY-08 | Web secure-context requirement documented; clear error if WebCrypto missing | Throws actionable error | jsTest where possible |
| REQ-CRY-09 | Keys non-portable across platforms — documented | CRYPTO.md section | Doc review checklist |
| REQ-CRY-10 | `KryptoLog` optional; default no-op; no klogging required | crypto module builds without composeApp logging | compile |

### 7.3 Serializers / envelope

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-SER-01 | Writes start with `SVBLENC1` + version byte `1` | Byte prefix asserts | jvmTest (port EncryptedSerializersTest) |
| REQ-SER-02 | Missing magic + `allowPlaintextRead=false` → CorruptionException | Exact behavior | jvmTest |
| REQ-SER-03 | Missing magic + `allowPlaintextRead=true` → decode plain inner | Migration path | jvmTest |
| REQ-SER-04 | Unknown envelope version → CorruptionException | — | jvmTest |
| REQ-SER-05 | Legacy AAD decrypt works; rewrite uses primary AAD | Read old / write new | jvmTest |
| REQ-SER-06 | `CancellationException` during decrypt propagates | Never CorruptionException | jvmTest (existing CipherFailureHandlingTest spirit) |
| REQ-SER-07 | Preferences encrypted serializer round-trip | Preferences equality | jvmTest |
| REQ-SER-08 | Proto encrypted serializer round-trip for `@Serializable` type | — | jvmTest |
| REQ-SER-09 | Empty payload → defaultValue | — | jvmTest |

### 7.4 Corruption

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-COR-01 | Fail-closed handler quarantines file to `*.corrupt` when FS available | File moved; exception rethrown; default not written | jvmTest (FailClosedCorruptionHandlerTest) |
| REQ-COR-02 | IndexedDB corruption does not silently return defaults without signaling | Defined behavior in API docs + test | jsTest |
| REQ-COR-03 | Library docs warn against replace-with-default as security footgun | MIGRATION/CRYPTO text | Doc checklist |

### 7.5 Storage / factories

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-STO-01 | File targets use OkioStorage + platform FileSystem | create + updateData round-trip | jvmTest |
| REQ-STO-02 | Web typed store uses IndexedDbStorage | Data persists across DataStore recreate in same origin | jsTest |
| REQ-STO-03 | Web encrypted prefs use WebLocalStorage | Persist in localStorage key space | jsTest |
| REQ-STO-04 | Plain prefs factory creates readable unencrypted Preferences | Can read without Cipher | jvmTest + jsTest |
| REQ-STO-05 | Encrypted factories refuse use before CryptoRuntime Ready (or document caller responsibility + sample enforces gate) | Sample keeps gate; lib documents contract | Sample test / doc |
| REQ-STO-06 | Migrations list is plumbed to DataStoreFactory | Custom DataMigration invoked | jvmTest |
| REQ-STO-07 | Preferences file extension rules respected (`.preferences_pb` where AndroidX requires) | Document + validate in Android factory | android test |

### 7.6 Rotation & re-encrypt

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-ROT-01 | `StoreRegistry.reEncryptAll` rewrites ciphertext under current Cipher | After forced rotation simulation, old ciphertext decryptable with new key path | jvmTest with fake Cipher/KeyRotator |
| REQ-ROT-02 | `CryptoRuntime.initialize` calls reEncrypt when rotator returns true | Order: rotate → reEncrypt → Ready | jvmTest |
| REQ-ROT-03 | Failed reEncrypt → Error state | — | jvmTest |
| REQ-ROT-04 | Android/JVM time-based rotator still configurable via KeyRotationConfig | Period honored in test with fake clock if possible | jvm/android test |
| REQ-ROT-05 | Web rotator remains honest no-op; documented | CRYPTO.md | Doc + unit returns false |

### 7.7 Android DX

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-AND-01 | `encryptedProtoDataStore` delegate singleton per fileName | Same instance | androidHostTest |
| REQ-AND-02 | `encryptedPreferencesDataStore` works | Round-trip | androidHostTest |
| REQ-AND-03 | `plainPreferencesDataStore` works | Round-trip | androidHostTest |
| REQ-AND-04 | Delegates never reference security-crypto | Compile + dep check | REQ-PKG-04 |

### 7.8 Migration artifact

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-MIG-01 | Documented migration from plaintext DataStore using `allowPlaintextRead` | Guide + test | jvmTest |
| REQ-MIG-02 | Best-effort reader for Tink AEAD blobs without magic (AeadSerializer-like) on Android | Test with fixture generated by Tink Aead | androidHostTest |
| REQ-MIG-03 | Document osipxd security-crypto users: migrate off EncryptedFile; provide guide even if automatic decrypt is limited | MIGRATION.md | Doc checklist |
| REQ-MIG-04 | ESP → Preferences DataStore helper or guide | At least guide; code if feasible without security-crypto | Doc / optional test |

### 7.9 Hardening (complete scope)

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-HRD-01 | Frozen blob fixtures per target for envelope v1 | Compat suite never broken without version bump | `compat` source set tests |
| REQ-HRD-02 | Binary compatibility validator on JVM public API | `.api` dump in CI | BCV task |
| REQ-HRD-03 | Optional StreamingAead encrypting path for large payloads (Android/JVM) | Feature flag / alternate serializer; documented | jvmTest |
| REQ-HRD-04 | iOS master key in Keychain | Keys survive reinstall policy as documented; sandbox file deprecated | ios test / manual protocol |
| REQ-HRD-05 | Public README quickstarts for Android, iOS, JVM, Web | Copy-paste works | Manual / sample |

### 7.10 Sample / consumer

| ID | Requirement | AC | Tests |
|----|-------------|----|-------|
| REQ-SMP-01 | composeApp uses kryptostore artifacts for crypto + encrypted stores | No duplicate Encrypted*Serializer in composeApp | compile + grep gate |
| REQ-SMP-02 | Remember-email uses **plain** preferences API | Logout → email restored when flag set | commonTest / UI test |
| REQ-SMP-03 | CRYPTO_KMP.md corrected (keys IndexedDB; fail-closed; AAD derive; rotation/re-encrypt) | Matches code | Doc review |
| REQ-SMP-04 | securePrefs has at least one intentional consumer OR is removed from Koin to avoid dead API | No orphan wiring without comment | Review |

---

## 8. Spec-Driven Development process

### 8.1 Spec change control

1. New behavior ⇒ add/change `REQ-*` + `DEC-*` if policy changes.
2. Implementation PRs must list REQ ids in the description.
3. “Drive-by” refactors without REQ are allowed only if they do not change observable behavior (and say so).

### 8.2 Definition of Ready (for a phase)

- REQs for the phase listed
- Tests named in §11 mapped
- Forbidden-deps check still green
- Owner not blocking on open DEC (none open)

### 8.3 Definition of Done (for a phase)

- All phase REQs AC checked
- Tests green on required CI matrix for that phase
- Docs touched if behavior/docs diverge
- Phase report written

---

## 9. Test-Driven Development process

### 9.1 Rules

1. For new REQs: add failing test(s) first when the behavior is unit-testable.
2. Prefer pure jvmTest for serializers/envelope/registry (fast feedback).
3. Platform tests for Cipher actuals and Storage.
4. Do not delete failing compat fixture tests to “make CI green” — bump envelope version instead.
5. Port and keep existing tests:

   - `EncryptedSerializersTest`
   - `CipherFailureHandlingTest`
   - `FailClosedCorruptionHandlerTest`

### 9.2 Test catalog (minimum)

| Test class (suggested) | Covers |
|------------------------|--------|
| `EnvelopeFormatTest` | REQ-SER-01..04, 09 |
| `AssociatedDataFallbackTest` | REQ-SER-05 |
| `DecryptCancellationTest` | REQ-SER-06 |
| `EncryptedPreferencesRoundTripTest` | REQ-SER-07 |
| `EncryptedProtoRoundTripTest` | REQ-SER-08 |
| `PlaintextMigrationOptionTest` | REQ-SER-02, 03, REQ-MIG-01 |
| `FailClosedCorruptionHandlerTest` | REQ-COR-01 |
| `CryptoRuntimeStateTest` | REQ-CRY-02, 03 |
| `StoreReEncryptTest` | REQ-ROT-01..03 |
| `ForbiddenDependenciesTest` | REQ-PKG-04, 05 |
| `IndexedDbStorageTest` (js) | REQ-STO-02 |
| `WebLocalStoragePreferencesTest` (js) | REQ-STO-03, 04 |
| `AndroidDelegateTest` | REQ-AND-01..03 |
| `CompatBlobFixtureTest` | REQ-HRD-01 |
| `RememberEmailPlainPrefsTest` (sample) | REQ-SMP-02 |

### 9.3 CI matrix (target)

| Task | Phase required from |
|------|---------------------|
| `jvmTest` on all kryptostore modules | Phase A+ |
| `androidHostTest` crypto + android | Phase B+ / D |
| `jsTest` storage + crypto bindings | Phase C+ |
| `iosSimulatorArm64Test` when Xcode present | Phase B+ (best effort locally) |
| BCV | Phase F (API freeze) |
| Forbidden dependency check | Phase A+ |

---

## 10. Implementation phases (action plan)

Execute in order. Each phase is a PR-sized unit unless owner batches.

### Phase 0 — Spec freeze & doc hygiene

**Work:**

- **criollo-kmp-foundation:** this file is already the living source of truth; ensure it is linked from `docs/index.md` + `mkdocs.yml` (and README “Upcoming” if desired).
- **saveable:** `docs/spec-final-kryptostore.md` is a pointer only; fix `CRYPTO_KMP.md` inaccuracies when starting consumer migration (or with Phase A consumer wire-up):
  - Keys: IndexedDB (not localStorage)
  - Reads: fail-closed by default (not “fall back to plain”)
  - AAD: `storeName|vN` + optional legacy

**AC:** Spec linked in foundation docs nav; saveable pointer present; DEC-31..33 acknowledged.

**REQs:** REQ-SMP-03 (partial), REQ-PKG-07 (docs portion).

### Phase A — Skeleton + crypto extract (**foundation** primary)

**Work (in criollo-kmp-foundation):**

- Follow `docs/adding-a-module.md`: create `:kryptostore:crypto`, wire `ProjectConfig` / namespaces / RootPlugin / BOM stub.
- Port code from `saveable/composeApp/core/crypto` (do not leave publishable API depending on composeApp logging/utils); introduce `KryptoLog`.
- Apply `criollo.kmp-library` conventions; JDK 21; Apple targets per foundation publishing docs.

**Work (in saveable, follow-up PR):**

- `includeBuild` / substitution → consume `:kryptostore:crypto`.
- Remove or thin-alias `:composeApp:core:crypto`.

**AC:**

- [ ] REQ-PKG-01, 02, 05, 06, 07
- [ ] REQ-CRY-01 (jvm + android at minimum), 02, 03, 07, 10
- [ ] saveable sample still initializes crypto and opens settings via includeBuild

**TDD:** CryptoRuntimeStateTest, Cipher round-trip, ForbiddenDependenciesTest skeleton (in foundation).

### Phase B — Serializers extract (**foundation**)

**Work:**

- Port envelope serializers + fail-closed helpers → `:kryptostore:serializers`.
- Public `EncryptedStoreOptions`, `allowPlaintextRead`.
- Port serializer tests from saveable; expand for REQ-SER-*.
- saveable depends on serializers artifact.

**AC:** REQ-SER-01..09, REQ-COR-01; saveable uses foundation serializers.

### Phase C — Core factories + web storage (**foundation**)

**Work:**

- `:kryptostore` core: factories, `IndexedDbStorage`, FileSystem expect/actual.
- Wire saveable typed settings to kryptostore factories (IndexedDB on web unchanged behavior).

**AC:** REQ-STO-01, 02, 05, 06; web proto still IndexedDB.

### Phase D — Preferences + plain API + saveable remember-email

**Work:**

- `:kryptostore:preferences` in foundation.
- remember-email on **plain** prefs in **saveable**.
- Resolve securePrefs orphan in saveable.

**AC:** REQ-STO-03, 04, 07; REQ-SMP-02, 04.

### Phase E — Rotation re-encrypt + Android delegates + streaming/Keychain (**foundation**)

**Work:** StoreRegistry, `:kryptostore:android`, StreamingAead optional, iOS Keychain.

**AC:** All ROT + AND + HRD-03/04 REQs.

### Phase F — BOM finalize, migrate-android, compat, BCV, publish (**foundation**)

**Work:** Extend foundation BOM; migrate-android; fixtures; BCV; docs on foundation site; `publishToMavenLocal` / Central via existing release workflows.

**AC:** REQ-PKG-03, 04; REQ-MIG-*; REQ-HRD-01, 02, 05; publish dry-run.

### Phase G — Stabilization

**Work:** Soak via saveable dogfood + foundation CI; release with release-please when ready.

**AC:** G1–G10 checklist in §1.3 all true.

---

## 11. Acceptance criteria — library “complete”

KryptoStore is **complete** when:

1. Another KMP repo can depend on `io.github.jdbenitez94.criollo.kmp.foundation:bom` + `kryptostore` + `kryptostore-preferences` and run encrypted proto + encrypted prefs + plain prefs on Android, JVM, iOS, JS, Wasm without copying saveable internals.
2. Web matrix holds: IndexedDB / localStorage / WebCrypto / keys in IndexedDB.
3. Forbidden deps absent.
4. Fail-closed default + opt-in plaintext migration.
5. Rotation re-encrypts registered stores on Android/JVM/iOS (Web documented limitation).
6. Android delegates ship without security-crypto.
7. Compat fixtures + BCV + MIGRATION docs exist.
8. composeApp is a consumer; remember-email uses plain prefs.
9. All REQ-* in §7 are Done or explicitly deferred by owner amendment to this file.

---

## 12. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| DataStore 1.3 alphas break consumers | DEC-20; pin in BOM; consider stable for Maven 1.0 |
| Re-encrypt bugs brick user data | Fail closed; quarantine; extensive ROT tests; backup guidance |
| Web rotation impossible with non-extractable keys | Honest no-op + docs; future multi-key design |
| IndexedDB vs OPFS future | DEC-10 stick to IndexedDB for v1 |
| Magic name `SVBLENC1` branding odd under KryptoStore | Keep for compatibility; document; envelope v2 only if breaking |
| Large extraction PR | Enforce phase gates |
| CRYPTO_KMP.md drift | Phase 0 / REQ-SMP-03 |

---

## 13. Phase checklist (for agents)

Copy into PR descriptions:

```text
Phase _: 
- [ ] REQs: …
- [ ] Tests added/updated (TDD): …
- [ ] Forbidden deps check green
- [ ] Sample still builds: …
- [ ] Docs updated: …
- [ ] Residual risks: …
```

---

## 14. Supersession notice

| Document | Role after this spec |
|----------|----------------------|
| `saveable/docs/encrypted-datastore-lib.md` | Historical design; follow **this** file |
| `saveable/docs/encrypted-datastore-conversation-handoff.md` | Rationale archive; decisions closed in §2 |
| `saveable/composeApp/docs/CRYPTO_KMP.md` | Must be corrected when saveable consumes kryptostore |
| `saveable/docs/spec-final-kryptostore.md` | Pointer only → this foundation doc |

---

## 15. Ready-to-paste prompt for the implementing agent

```text
You are implementing KryptoStore as part of Criollo KMP infrastructure.

HOME REPO (implementation lands here):
/Users/jbenitez/Projects/criollo-kmp-foundation
GitHub: https://github.com/jdbenitez94/criollo-kmp-foundation
(There is no criollo-kmp-infrastructure directory — use foundation.)

DONOR / DOGFOOD REPO:
/Users/jbenitez/Projects/saveable
(source of crypto/datastore code to port; composeApp becomes consumer via includeBuild)

AUTHORITATIVE SPEC (read completely first):
/Users/jbenitez/Projects/criollo-kmp-foundation/docs/spec-final-kryptostore.md

Also read:
criollo-kmp-foundation/docs/adding-a-module.md
criollo-kmp-foundation/docs/publishing.md
criollo-kmp-foundation/README.md

Supporting context only (do not override the spec):
saveable/docs/encrypted-datastore-conversation-handoff.md
saveable/docs/encrypted-datastore-lib.md
saveable/composeApp/docs/CRYPTO_KMP.md (stale until consumer Phase 0 hygiene)
saveable/local-extra/encrypted-datastore-1.1.1-beta03/ (reference only — do NOT depend on it)

Hard constraints:
- Product KryptoStore; Maven group io.github.jdbenitez94.criollo.kmp.foundation
- Artifacts kryptostore-* under foundation; packages …foundation.kryptostore.*
- Use criollo.kmp-library / ProjectConfig / existing BOM (extend :bom — no second BOM)
- No androidx.security:security-crypto, no datastore-tink on kryptostore classpaths
- OkioSerializer pipeline only in core
- Web: proto=IndexedDB, prefs=localStorage, crypto=WebCrypto, keys in IndexedDB
- Fail-closed default; allowPlaintextRead opt-in
- Keep wire magic SVBLENC1 + envelope v1
- SDD+TDD: every change maps to REQ-* ids; tests first where practical
- Conventional Commits; no AI co-author; no commit/push unless I ask
- JDK 21; follow foundation CI/publish (Apple targets per publishing.md)

Start with Phase 0 then Phase A from the spec.
Primary commits in criollo-kmp-foundation; saveable PRs only for consumer migration + CRYPTO_KMP.
After each phase, output the phase checklist (§13) and stop for review unless I say to continue.
```

---

## 16. Document history

| Date | Change |
|------|--------|
| 2026-08-22 | Initial authoritative SDD/TDD spec; closed DEC-01..30; named KryptoStore |
| 2026-08-22 | Amended: home = criollo-kmp-foundation; group/packages/BOM/phases/prompt; DEC-31..33 |
| 2026-08-22 | Phase 0: living copy in foundation `docs/`; saveable file reduced to pointer |
