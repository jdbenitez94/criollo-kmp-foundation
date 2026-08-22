@file:OptIn(ExperimentalEncodingApi::class, ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

actual fun createPlatformCryptoStack(appId: String, @Suppress("UNUSED_PARAMETER") rotationConfig: KeyRotationConfig): PlatformCryptoStack {
    // Web rotator is an honest no-op; KeyRotationConfig is ignored (REQ-ROT-05).
    val keyAlias = "$appId.webcrypto.aes_gcm.v1"
    return PlatformCryptoStack(
        cipher = WebCryptoCipher(keyAlias),
        keyRotator = WebCryptoKeyRotator(keyAlias),
        postRotationInit = { ensureWebCryptoKey(keyAlias) },
    )
}

private class WebCryptoCipher(private val keyAlias: String) : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val plaintextBase64 = Base64.encode(message)
        val associatedDataBase64 = associatedData?.let(Base64::encode)
        validateWebCryptoInput(keyAlias, plaintextBase64, associatedDataBase64)
        ensureWebCryptoHelpersInstalled()
        val ciphertext = CryptoBindings.encrypt(
            keyAlias = keyAlias,
            plaintextBase64 = plaintextBase64,
            associatedDataBase64 = associatedDataBase64,
        ).awaitString()
        return Base64.decode(ciphertext)
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        val ciphertextBase64 = Base64.encode(message)
        val associatedDataBase64 = associatedData?.let(Base64::encode)
        validateWebCryptoInput(keyAlias, ciphertextBase64, associatedDataBase64)
        ensureWebCryptoHelpersInstalled()
        val plaintext = CryptoBindings.decrypt(
            keyAlias = keyAlias,
            ciphertextBase64 = ciphertextBase64,
            associatedDataBase64 = associatedDataBase64,
        ).awaitString()
        return Base64.decode(plaintext)
    }
}

private class WebCryptoKeyRotator(private val keyAlias: String) : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean {
        ensureWebCryptoKey(keyAlias)
        return false
    }
}

internal actual fun randomPlatformAesKey(): ByteArray = error("Raw WebCrypto key export is disabled for web targets.")

internal actual suspend fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray, associatedData: ByteArray?): ByteArray =
    error("Raw-key AES-GCM is disabled for web targets. Use WebCryptoCipher.")

internal actual suspend fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, associatedData: ByteArray?): ByteArray =
    error("Raw-key AES-GCM is disabled for web targets. Use WebCryptoCipher.")

private suspend fun ensureWebCryptoKey(keyAlias: String) {
    require(keyAlias.isNotBlank()) { "WebCrypto key alias cannot be blank." }
    ensureWebCryptoHelpersInstalled()
    CryptoBindings.ensureKey(keyAlias).await()
}

private fun validateWebCryptoInput(keyAlias: String, payloadBase64: String, associatedDataBase64: String?) {
    require(keyAlias.isNotBlank()) { "WebCrypto key alias cannot be blank." }
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
