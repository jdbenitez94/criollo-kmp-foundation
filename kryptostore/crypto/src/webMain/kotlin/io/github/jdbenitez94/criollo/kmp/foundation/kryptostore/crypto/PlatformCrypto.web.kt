@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

actual fun createPlatformCryptoStack(appId: String, rotationConfig: KeyRotationConfig): PlatformCryptoStack {
    require(appId.isNotBlank()) { "WebCrypto appId cannot be blank." }
    return PlatformCryptoStack(
        cipher = WebCryptoCipher(appId),
        keyRotator = WebCryptoKeyRotator(appId, rotationConfig),
        postRotationInit = { ensureWebCryptoKeyring(appId) },
        postMigrationCleanup = { deleteInactiveWebCryptoKeys(appId) },
    )
}

private class WebCryptoCipher(private val appId: String) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val plaintextBase64 = Base64.encode(message)
        val associatedDataBase64 = associatedData?.let(Base64::encode)
        validateWebCryptoInput(appId, plaintextBase64, associatedDataBase64)
        ensureWebCryptoHelpersInstalled()
        val ciphertext = CryptoBindings.encrypt(
            appId = appId,
            plaintextBase64 = plaintextBase64,
            associatedDataBase64 = associatedDataBase64,
        ).awaitString()
        return Base64.decode(ciphertext)
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val ciphertextBase64 = Base64.encode(message)
        val associatedDataBase64 = associatedData?.let(Base64::encode)
        validateWebCryptoInput(appId, ciphertextBase64, associatedDataBase64)
        ensureWebCryptoHelpersInstalled()
        val plaintext = CryptoBindings.decrypt(
            appId = appId,
            ciphertextBase64 = ciphertextBase64,
            associatedDataBase64 = associatedDataBase64,
        ).awaitString()
        return Base64.decode(plaintext)
    }
}

@OptIn(ExperimentalTime::class)
private class WebCryptoKeyRotator(
    private val appId: String,
    private val rotationConfig: KeyRotationConfig,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean {
        ensureWebCryptoHelpersInstalled()
        val result = CryptoBindings.rotateIfNeeded(
            appId = appId,
            periodMs = rotationConfig.rotationPeriod.inWholeMilliseconds.toDouble(),
            nowMillis = nowMillis().toDouble(),
        ).await()
        return result.toString().equals("true", ignoreCase = true)
    }
}

internal actual fun randomPlatformAesKey(): ByteArray = error("Raw WebCrypto key export is disabled for web targets.")

internal actual suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray =
    error("Raw-key AES-GCM is disabled for web targets. Use WebCryptoCipher.")

internal actual suspend fun aesGcmDecrypt(
    key: ByteArray,
    ciphertext: ByteArray,
    associatedData: ByteArray?,
): ByteArray = error("Raw-key AES-GCM is disabled for web targets. Use WebCryptoCipher.")

private suspend fun ensureWebCryptoKeyring(appId: String) {
    require(appId.isNotBlank()) { "WebCrypto appId cannot be blank." }
    ensureWebCryptoHelpersInstalled()
    CryptoBindings.ensureKeyring(appId).await()
}

private suspend fun deleteInactiveWebCryptoKeys(appId: String) {
    ensureWebCryptoHelpersInstalled()
    val activeKeyId = CryptoBindings.getActiveKeyId(appId).awaitString()
    val keyIdsJson = CryptoBindings.listKeyIds(appId).awaitString()
    parseJsonStringArray(keyIdsJson)
        .filter { it != activeKeyId }
        .forEach { keyId -> CryptoBindings.deleteKey(appId, keyId).await() }
}

private fun validateWebCryptoInput(appId: String, payloadBase64: String, associatedDataBase64: String?) {
    require(appId.isNotBlank()) { "WebCrypto appId cannot be blank." }
    require(payloadBase64.all(Char::isAscii)) { "WebCrypto payload must be Base64 ASCII." }
    require(associatedDataBase64 == null || associatedDataBase64.all(Char::isAscii)) {
        "Associated data must be Base64 ASCII."
    }
}

private fun Char.isAscii(): Boolean = code <= ASCII_MAX_CODE

private const val ASCII_MAX_CODE = 0x7F

private suspend fun Promise<JsAny?>.awaitString(): String = await().toString()

private var webCryptoHelpersInstalled = false

private fun ensureWebCryptoHelpersInstalled() {
    if (!webCryptoHelpersInstalled) {
        CryptoBindings.install()
        webCryptoHelpersInstalled = true
    }
}
