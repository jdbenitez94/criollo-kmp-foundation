# KryptoStore crypto

Platform crypto for encrypted DataStore (Tink / Android Keystore / WebCrypto / iOS Keychain).

See also the module notes in `kryptostore/crypto/CRYPTO.md`.

## Rotation

| Target | Behavior |
| -------- | ---------- |
| Android / JVM | Time-based Tink keyset rotation via `KeyRotationConfig` (default 90 days) |
| iOS | Time-based AES-GCM key rotation; master keys in **Keychain** (not sandbox files) |
| Web (JS/Wasm) | Multi-key WebCrypto keyring in IndexedDB (non-extractable keys + envelope); honors `KeyRotationConfig` and re-encrypts registered stores (REQ-ROT-05) |

After rotation, `CryptoRuntime.initialize` runs in strict order:
`rotateKeyIfNeeded` → `postRotationInit` → `StoreRegistry.reEncryptAll` (when rotated)
→ `postMigrationCleanup` → `Ready`. Initialization failures surface `CryptoRuntimeState.Error`
(and a subsequent `initialize()` call retries).

## Android initialization

When creating `createPlatformCryptoStack` or stores from `commonMain` on Android,
call `KryptostoreAndroid.initialize(context)` once in `Application.onCreate`.
It initializes both `AndroidCryptoContextHolder` (for Keystore-backed keysets)
and `KryptostorePaths` (for DataStore file paths). If you do not depend on
`kryptostore-android-delegates`, call `AndroidCryptoContextHolder.init(context)`
and `installKryptostoreAndroidFilesDir(filesDir)` directly.

## Web Secure Context (REQ-CRY-08)

On JS and WasmJS targets, KryptoStore relies on the browser WebCrypto API (`crypto.subtle`)
inside a Web Worker and stores non-extractable `CryptoKey` handles in IndexedDB (`app-crypto`).
This requires a **Secure Context** (`https://` or `http://localhost`); insecure origins fail
closed with an explicit error.

## Key Non-Portability (REQ-CRY-09 / NG1)

Hardware/OS-backed master keys (Android Keystore, iOS Keychain `ThisDeviceOnly`,
macOS Keychain, and WebCrypto `extractable: false` keys in IndexedDB) are
**bound to the local device/browser profile and are non-portable by design**.
Encrypted DataStore blobs cannot be copied across devices or restored onto a different
device without an application-level plaintext export/re-import flow.

## Streaming AEAD (optional) — REQ-HRD-03

On **Android / JVM only**, `StreamingAeadCipher` wraps a Tink `StreamingAead`
primitive (`AES256_GCM_HKDF_1MB`) for multi-MB payloads.

> **Important:** `createStreamingAeadCipher()` generates a fresh **in-memory** keyset
> on each invocation (intended for ephemeral/session streams or tests). For persistent
> `DataStore`s across process restarts, construct `StreamingAeadCipher(streamingAead)` using
> a `StreamingAead` primitive obtained from a persisted Tink `KeysetHandle`.

```kotlin
// Ephemeral in-memory cipher (tests / transient buffers):
val ephemeralCipher: Cipher = createStreamingAeadCipher()

// Persistent store (bring a StreamingAead primitive from a persisted KeysetHandle):
val persistentCipher: Cipher = StreamingAeadCipher(persistedKeysetHandle.getPrimitive(StreamingAead::class.java))
val store = createEncryptedProtoDataStore(
    cipher = persistentCipher,
    kSerializer = LargeBlob.serializer(),
    defaultValue = LargeBlob(),
    locator = StoreLocator.platform(
        producePath = { KryptostorePaths.file("large.pb") },
        name = "large",
    ),
)
```

### When not to use it

- Small settings / preferences — prefer `createPlatformCryptoStack` + default `Cipher`.
- Preferences factories — Streaming AEAD is supported as a `Cipher` plug-in but is overkill for prefs.
- JS / Wasm / iOS — not available; use the platform default cipher.

Covered by `StreamingAeadCipherTest` (jvmTest).

## iOS Keychain — REQ-HRD-04

Master AES keys use the iOS Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`)
via `IosKeychain` / `createPlatformCryptoStack`. Sandbox-file master keys are **not** used.

### Manual verification protocol (release gate)

1. Build an iOS app (or sample) that calls `createPlatformCryptoStack(appId)` → `CryptoRuntime.initialize()` → encrypt/decrypt a known blob.
2. Kill the app, relaunch, confirm decrypt still succeeds (same Keychain item).
3. Confirm no master-key file appears under the app sandbox Documents/Library for the crypto service.
4. Optional: uninstall + reinstall — expect **new** key (ThisDeviceOnly + uninstall clears Keychain for the app) and inability to decrypt prior blobs (documented fail-closed).

When a macOS CI runner with Xcode is available, run:

```bash
./gradlew :kryptostore:crypto:iosSimulatorArm64Test -Pcriollo.requireAppleTargets=true
```

(Add a smoke test that initializes the stack and round-trips when expanding CI.)
