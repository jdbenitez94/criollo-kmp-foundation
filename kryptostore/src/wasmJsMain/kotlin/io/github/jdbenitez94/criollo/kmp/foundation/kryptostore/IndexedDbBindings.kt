@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@JsFun(INDEXED_DB_INIT_FUNCTION)
private external fun jsInitIndexedDb()

@JsFun("() => globalThis.__kryptostore_idb.install()")
private external fun jsInstall()

@JsFun("(name) => globalThis.__kryptostore_idb.read(name)")
private external fun jsRead(name: String): Promise<JsAny?>

@JsFun("(name, payload) => globalThis.__kryptostore_idb.write(name, payload)")
private external fun jsWrite(name: String, payloadBase64: String): Promise<JsAny?>

@JsFun("(name) => globalThis.__kryptostore_idb.version(name)")
private external fun jsVersion(name: String): Promise<JsAny?>

@JsFun("(name) => globalThis.__kryptostore_idb.incrementVersion(name)")
private external fun jsIncrementVersion(name: String): Promise<JsAny?>

@JsFun("(name) => globalThis.__kryptostore_idb.lock(name)")
private external fun jsLock(name: String): Promise<JsAny?>

@JsFun("(name) => globalThis.__kryptostore_idb.tryLock(name)")
private external fun jsTryLock(name: String): Promise<JsAny?>

@JsFun("(name) => globalThis.__kryptostore_idb.unlock(name)")
private external fun jsUnlock(name: String): Promise<JsAny?>

internal actual object IndexedDbBindings {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            jsInitIndexedDb()
            initialized = true
        }
    }

    actual fun install() {
        ensureInitialized()
        jsInstall()
    }

    actual fun read(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsRead(name)
    }

    actual fun write(name: String, payloadBase64: String): Promise<JsAny?> {
        ensureInitialized()
        return jsWrite(name, payloadBase64)
    }

    actual fun version(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsVersion(name)
    }

    actual fun incrementVersion(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsIncrementVersion(name)
    }

    actual fun lock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsLock(name)
    }

    actual fun tryLock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsTryLock(name)
    }

    actual fun unlock(name: String): Promise<JsAny?> {
        ensureInitialized()
        return jsUnlock(name)
    }
}
