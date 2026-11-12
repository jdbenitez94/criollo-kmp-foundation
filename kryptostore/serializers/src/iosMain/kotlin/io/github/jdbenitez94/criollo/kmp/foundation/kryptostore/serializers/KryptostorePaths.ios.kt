@file:OptIn(ExperimentalForeignApi::class)

package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual fun resolveDataStoreFile(fileName: String): Path {
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String
        ?: error("NSDocumentDirectory is unavailable; cannot resolve KryptostorePaths")
    val dir = "$documents/datastore".toPath()
    FileSystem.SYSTEM.createDirectories(dir)
    return dir / fileName
}
