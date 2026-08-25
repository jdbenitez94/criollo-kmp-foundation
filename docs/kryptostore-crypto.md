# KryptoStore crypto

Platform crypto for encrypted DataStore (Tink / Android Keystore / WebCrypto / iOS Keychain).

See also the module notes in `kryptostore/crypto/CRYPTO.md`.

## Rotation

| Target | Behavior |
| -------- | ---------- |
| Android / JVM | Time-based Tink keyset rotation via `KeyRotationConfig` (default 90 days) |
| iOS | Time-based AES-GCM key rotation; master keys in **Keychain** (not sandbox files) |
| Web (JS/Wasm) | Time-based multi-key WebCrypto keyring in IndexedDB (non-extractable keys + key-id envelope); honors `KeyRotationConfig` and re-encrypts registered stores (REQ-ROT-05) |

After rotation, `CryptoRuntime.initialize` calls `StoreRegistry.reEncryptAll` before Ready. Failures surface `CryptoRuntimeState.Error`.

## Streaming AEAD (optional) — REQ-HRD-03

On **Android / JVM only**, use `createStreamingAeadCipher()` when payloads are large (multi‑MB) and you want Tink Streaming AEAD (`AES256_GCM_HKDF_1MB`).

```kotlin
val cipher: Cipher = createStreamingAeadCipher()
val store = createEncryptedProtoDataStore(
    cipher = cipher,
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

Master AES keys use the iOS Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`) via `IosKeychain` / `createPlatformCryptoStack`. Sandbox-file master keys are **not** used.

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
