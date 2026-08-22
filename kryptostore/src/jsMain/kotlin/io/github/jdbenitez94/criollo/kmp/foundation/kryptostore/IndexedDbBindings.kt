@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.JsNonModule
import kotlin.js.Promise

@JsModule("indexeddb.ts")
@JsNonModule
internal actual external object IndexedDbBindings {
    actual fun install()

    actual fun read(name: String): Promise<JsAny?>

    actual fun write(name: String, payloadBase64: String): Promise<JsAny?>

    actual fun version(name: String): Promise<JsAny?>

    actual fun incrementVersion(name: String): Promise<JsAny?>

    actual fun lock(name: String): Promise<JsAny?>

    actual fun tryLock(name: String): Promise<JsAny?>

    actual fun unlock(name: String): Promise<JsAny?>
}
