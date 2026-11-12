package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/** Magic header prepended to encrypted DataStore payloads (historical wire name; keep for blob compat). */
const val ENCRYPTED_BLOB_MAGIC = "SVBLENC1"

/** IndexedDB database name used by the WebCrypto worker — keys must not be stored in localStorage. */
const val WEB_CRYPTO_DB_NAME = "app-crypto"

/**
 * Bundle of platform-specific cryptographic primitives and lifecycle hooks consumed by [CryptoRuntime].
 */
data class PlatformCryptoStack(
    val cipher: Cipher,
    val keyRotator: KeyRotator,
    val postRotationInit: suspend () -> Unit = {
    },
    val postMigrationCleanup: suspend () -> Unit = {},
)

/**
 * Creates the default platform [PlatformCryptoStack] for [appId].
 *
 * On Android, call `KryptostoreAndroid.initialize(context)` (or `AndroidCryptoContextHolder.init(context)`)
 * in `Application.onCreate` before invoking this function.
 *
 * [rotationConfig] controls time-based key rotation on Android/JVM/iOS/Web (REQ-ROT-04, REQ-ROT-05).
 * Web uses a multi-key WebCrypto keyring in IndexedDB (non-extractable keys + key-id envelope).
 */
expect fun createPlatformCryptoStack(
    appId: String,
    rotationConfig: KeyRotationConfig = KeyRotationConfig.DEFAULT,
): PlatformCryptoStack
