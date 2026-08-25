package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal actual fun resolveDataStoreFile(fileName: String): Path {
    val home = System.getProperty("user.home")
        ?: error("user.home is not set; cannot resolve KryptostorePaths")
    val dir = "$home/.kryptostore/datastore".toPath()
    FileSystem.SYSTEM.createDirectories(dir)
    return dir / fileName
}
