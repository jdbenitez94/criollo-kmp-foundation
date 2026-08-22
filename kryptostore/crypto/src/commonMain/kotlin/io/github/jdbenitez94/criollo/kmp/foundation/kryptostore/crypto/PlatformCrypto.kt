package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/** Magic header prepended to encrypted DataStore payloads (historical wire name; keep for blob compat). */
const val ENCRYPTED_BLOB_MAGIC = "SVBLENC1"

/** IndexedDB database name used by the WebCrypto worker — keys must not be stored in localStorage. */
const val WEB_CRYPTO_DB_NAME = "app-crypto"

data class PlatformCryptoStack(val cipher: Cipher, val keyRotator: KeyRotator, val postRotationInit: suspend () -> Unit = {}, val postMigrationCleanup: suspend () -> Unit = {})

/**
 * Platform entry: Android needs [AndroidCryptoContextHolder] registration before use.
 *
 * [rotationConfig] controls time-based key rotation on Android/JVM/iOS (REQ-ROT-04).
 * Web ignores the period and always returns false from the rotator (REQ-ROT-05).
 */
expect fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig = KeyRotationConfig.DEFAULT): PlatformCryptoStack
