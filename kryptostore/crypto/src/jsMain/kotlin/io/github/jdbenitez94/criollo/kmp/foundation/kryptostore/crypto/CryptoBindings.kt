@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal actual object CryptoBindings {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            evalScript(CRYPTO_INIT_SCRIPT)
            initialized = true
        }
    }

    @Suppress("UnusedParameter")
    private fun evalScript(script: String) {
        js("eval(script)")
    }

    actual fun install() {
        ensureInitialized()
        js("globalThis.__kryptostore_crypto.install()")
    }

    actual fun ensureKeyring(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.ensureKeyring(appId)")
    }

    actual fun encrypt(appId: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.encrypt(appId, plaintextBase64, associatedDataBase64)")
    }

    actual fun decrypt(appId: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.decrypt(appId, ciphertextBase64, associatedDataBase64)")
    }

    actual fun rotateIfNeeded(appId: String, periodMs: Double, nowMillis: Double): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.rotateIfNeeded(appId, periodMs, nowMillis)")
    }

    actual fun listKeyIds(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.listKeyIds(appId)")
    }

    actual fun deleteKey(appId: String, keyId: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.deleteKey(appId, keyId)")
    }

    actual fun getActiveKeyId(appId: String): Promise<JsAny?> {
        ensureInitialized()
        return js("globalThis.__kryptostore_crypto.getActiveKeyId(appId)")
    }
}
