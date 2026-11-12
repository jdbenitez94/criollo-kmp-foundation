@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@JsFun(CRYPTO_INIT_FUNCTION)
private external fun jsInitCrypto()

@JsFun("() => globalThis.__kryptostore_crypto.install()")
private external fun jsInstall()

@JsFun("(appId) => globalThis.__kryptostore_crypto.ensureKeyring(appId)")
private external fun jsEnsureKeyring(appId: String): Promise<JsAny?>

@JsFun("(appId, plaintext, aad) => globalThis.__kryptostore_crypto.encrypt(appId, plaintext, aad)")
private external fun jsEncrypt(appId: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

@JsFun("(appId, ciphertext, aad) => globalThis.__kryptostore_crypto.decrypt(appId, ciphertext, aad)")
private external fun jsDecrypt(appId: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

@JsFun("(appId, period, now) => globalThis.__kryptostore_crypto.rotateIfNeeded(appId, period, now)")
private external fun jsRotateIfNeeded(appId: String, periodMs: Double, nowMillis: Double): Promise<JsAny?>

@JsFun("(appId) => globalThis.__kryptostore_crypto.listKeyIds(appId)")
private external fun jsListKeyIds(appId: String): Promise<JsAny?>

@JsFun("(appId, keyId) => globalThis.__kryptostore_crypto.deleteKey(appId, keyId)")
private external fun jsDeleteKey(appId: String, keyId: String): Promise<JsAny?>

@JsFun("(appId) => globalThis.__kryptostore_crypto.getActiveKeyId(appId)")
private external fun jsGetActiveKeyId(appId: String): Promise<JsAny?>

internal actual object CryptoBindings {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            jsInitCrypto()
            initialized = true
        }
    }

    actual fun install() {
        ensureInitialized()
        jsInstall()
    }

    actual fun ensureKeyring(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return jsEnsureKeyring(appId)
    }

    actual fun encrypt(appId: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?> {
        ensureInitialized()
        return jsEncrypt(appId, plaintextBase64, associatedDataBase64)
    }

    actual fun decrypt(appId: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?> {
        ensureInitialized()
        return jsDecrypt(appId, ciphertextBase64, associatedDataBase64)
    }

    actual fun rotateIfNeeded(appId: String, periodMs: Double, nowMillis: Double): Promise<JsAny?> {
        ensureInitialized()
        return jsRotateIfNeeded(appId, periodMs, nowMillis)
    }

    actual fun listKeyIds(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return jsListKeyIds(appId)
    }

    actual fun deleteKey(appId: String, keyId: String): Promise<JsAny?> {
        ensureInitialized()
        return jsDeleteKey(appId, keyId)
    }

    actual fun getActiveKeyId(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return jsGetActiveKeyId(appId)
    }
}
