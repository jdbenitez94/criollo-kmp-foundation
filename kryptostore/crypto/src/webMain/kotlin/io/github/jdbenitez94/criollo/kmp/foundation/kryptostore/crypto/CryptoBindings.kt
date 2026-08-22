@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal expect object CryptoBindings {
    fun install()

    fun ensureKey(keyAlias: String): Promise<JsAny?>

    fun encrypt(keyAlias: String, plaintextBase64: String, associatedDataBase64: String?): Promise<JsAny?>

    fun decrypt(keyAlias: String, ciphertextBase64: String, associatedDataBase64: String?): Promise<JsAny?>
}
