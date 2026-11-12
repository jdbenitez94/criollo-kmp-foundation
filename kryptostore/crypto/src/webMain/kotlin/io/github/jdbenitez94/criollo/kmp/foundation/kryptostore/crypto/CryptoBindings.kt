@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal expect object CryptoBindings {
    fun install()

    fun ensureKeyring(appId: String): Promise<JsAny?>

    fun encrypt(appId: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    fun decrypt(appId: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    fun rotateIfNeeded(appId: String, periodMs: Double, nowMillis: Double): Promise<JsAny?>

    fun listKeyIds(appId: String): Promise<JsAny?>

    fun deleteKey(appId: String, keyId: String): Promise<JsAny?>

    fun getActiveKeyId(appId: String): Promise<JsAny?>
}
