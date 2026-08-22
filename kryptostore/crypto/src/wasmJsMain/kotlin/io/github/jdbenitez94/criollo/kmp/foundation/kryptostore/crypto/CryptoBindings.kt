@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule
import kotlin.js.Promise

@JsModule("crypto.ts")
internal actual external object CryptoBindings {
    actual fun install()

    actual fun ensureKey(keyAlias: String): Promise<JsAny?>

    actual fun encrypt(keyAlias: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    actual fun decrypt(keyAlias: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?>
}
