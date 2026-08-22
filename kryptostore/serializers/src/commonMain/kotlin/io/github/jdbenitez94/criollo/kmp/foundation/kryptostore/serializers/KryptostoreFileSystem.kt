package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.FileSystem

/** Platform Okio [FileSystem] for quarantine / file-backed stores; null on web. */
val kryptostoreFileSystem: FileSystem?
    get() = platformKryptostoreFileSystem

internal expect val platformKryptostoreFileSystem: FileSystem?
