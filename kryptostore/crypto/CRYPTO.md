# KryptoStore crypto

Platform crypto for encrypted DataStore (Tink / Android Keystore / WebCrypto / iOS Keychain).

## Rotation

| Target | Behavior |
|--------|----------|
| Android / JVM | Time-based Tink keyset rotation via [KeyRotationConfig] (default 90 days) |
| iOS | Time-based AES-GCM key rotation; master keys in **Keychain** (not sandbox files) |
| Web (JS/Wasm) | **Honest no-op** — `KeyRotator.rotateKeyIfNeeded()` always returns `false` after ensuring the WebCrypto key exists. There is no multi-key scheme yet; do not expect ciphertext rewrite on a schedule. |

After a successful rotation, [CryptoRuntime.initialize] calls [StoreRegistry.reEncryptAll] before Ready so registered stores rewrite under the new primary key. Failures surface [CryptoRuntimeState.Error].

Register stores with `registry.register(dataStore.asEncryptedStoreHandle())` or pass `registry=` to encrypted factories.

## Streaming AEAD (optional)

For large payloads on Android/JVM, use [createStreamingAeadCipher] instead of the default platform AEAD [Cipher]. Small settings should keep the default path.

## iOS Keychain (REQ-HRD-04)

Master AES keys live in the iOS Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`). Sandbox-file master keys are not used.
