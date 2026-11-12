package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.FileSystem

/** Platform Okio [FileSystem] for quarantine / file-backed stores; null on web. */
val kryptostoreFileSystem: FileSystem?
    get() = platformKryptostoreFileSystem

/**
 * Resolves the platform [FileSystem] or fails closed with the shared web-routing message.
 * Accepts an override so JVM tests can exercise the null branch without JS Kover merge.
 */
fun requireKryptostoreFileSystem(fileSystem: FileSystem? = kryptostoreFileSystem): FileSystem = fileSystem
    ?: error(
        "FileSystem is not available on this platform; " +
            "use StoreLocator.platform(..., name) or StoreLocator.named on JS/Wasm.",
    )

internal expect val platformKryptostoreFileSystem: FileSystem?

/**
 * True when [StoreLocator.Platform] should use Okio paths; false on JS/Wasm (named web storage).
 * Independent of [kryptostoreFileSystem] so routing is an explicit platform contract (DEC-35).
 */
internal expect val kryptostoreUsesFileStorage: Boolean
