# KryptoStore crypto

Platform crypto for encrypted DataStore (Tink / Android Keystore / WebCrypto / iOS Keychain).

See also the module notes in `kryptostore/crypto/CRYPTO.md`.

## Rotation

| Target | Behavior |
|--------|----------|
| Android / JVM | Time-based Tink keyset rotation via `KeyRotationConfig` (default 90 days) |
| iOS | Time-based AES-GCM key rotation; master keys in **Keychain** (not sandbox files) |
| Web (JS/Wasm) | **Honest no-op** — rotator returns `false` after ensuring the WebCrypto key exists (no multi-key scheme yet) |

After rotation, `CryptoRuntime.initialize` calls `StoreRegistry.reEncryptAll` before Ready. Failures surface `CryptoRuntimeState.Error`.

## Streaming AEAD (optional)

On Android/JVM, `createStreamingAeadCipher()` provides a `Cipher` for large payloads. Prefer the default platform cipher for small settings.

## iOS Keychain

Master AES keys use the iOS Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`). Sandbox-file master keys are not used.
