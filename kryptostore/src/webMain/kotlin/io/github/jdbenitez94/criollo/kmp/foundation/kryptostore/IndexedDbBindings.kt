@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

internal expect object IndexedDbBindings {
    fun install()

    fun read(name: String): Promise<JsAny?>

    fun write(name: String, payloadBase64: String): Promise<JsAny?>

    fun version(name: String): Promise<JsAny?>

    fun incrementVersion(name: String): Promise<JsAny?>

    fun lock(name: String): Promise<JsAny?>

    fun tryLock(name: String): Promise<JsAny?>

    fun unlock(name: String): Promise<JsAny?>
}
