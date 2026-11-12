# KryptoStore crypto

Platform crypto for encrypted DataStore (Tink / Android Keystore / WebCrypto / iOS Keychain).

## Rotation

| Target | Behavior |
| -------- | ---------- |
| Android / JVM | Time-based Tink keyset rotation via [KeyRotationConfig] (default 90 days) |
| iOS | Time-based AES-GCM key rotation; master keys in **Keychain** (not sandbox files) |
| Web (JS/Wasm) | Multi-key WebCrypto keyring in IndexedDB (`extractable: false`); `KeyRotationConfig` honored; re-encrypts stores then cleans inactive keys (REQ-ROT-05) |

After a successful rotation, [CryptoRuntime.initialize] runs `postRotationInit`,
calls [StoreRegistry.reEncryptAll], and then runs `postMigrationCleanup` before
transitioning to `Ready`. Initialization failures surface [CryptoRuntimeState.Error].

Register stores with `registry.register(dataStore.asEncryptedStoreHandle())` or pass `registry=` to encrypted factories.

## Streaming AEAD (optional)

For large payloads on Android/JVM, construct [StreamingAeadCipher] with a Tink
`StreamingAead` primitive backed by a persisted keyset (`createStreamingAeadCipher()`
creates an in-memory ephemeral keyset for tests/transient streams).
Small settings should keep the default platform [Cipher].

## Web Secure Context (REQ-CRY-08) & Key Non-Portability (REQ-CRY-09)

- **Web Secure Context:** JS/Wasm WebCrypto requires `https://` or `http://localhost`.
  Master keys are stored as non-extractable `CryptoKey` objects in IndexedDB
  (`app-crypto`), never `localStorage`.
- **Non-portable keys:** Platform master keys (Android Keystore, iOS Keychain
  `ThisDeviceOnly`, macOS Keychain, WebCrypto `extractable: false`) are device-bound
  and cannot be exported across devices.

## iOS Keychain (REQ-HRD-04)

Master AES keys live in the iOS Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`). Sandbox-file master keys are not used.
