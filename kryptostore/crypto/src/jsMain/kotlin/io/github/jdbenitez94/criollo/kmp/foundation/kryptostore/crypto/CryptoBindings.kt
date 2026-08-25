@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule
import kotlin.js.JsNonModule
import kotlin.js.Promise

@JsModule("./crypto.js")
@JsNonModule
internal actual external object CryptoBindings {
    actual fun install()

    actual fun ensureKeyring(appId: String): Promise<JsAny?>

    actual fun encrypt(appId: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    actual fun decrypt(appId: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    actual fun rotateIfNeeded(appId: String, periodMs: Double, nowMillis: Double): Promise<JsAny?>

    actual fun listKeyIds(appId: String): Promise<JsAny?>

    actual fun deleteKey(appId: String, keyId: String): Promise<JsAny?>

    actual fun getActiveKeyId(appId: String): Promise<JsAny?>
}
