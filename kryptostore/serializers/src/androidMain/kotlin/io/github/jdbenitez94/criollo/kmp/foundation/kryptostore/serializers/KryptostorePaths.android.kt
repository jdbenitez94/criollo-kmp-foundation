package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.FileSystem
import okio.Path

/**
 * Application [filesDir] root set by kryptostore-android [KryptostoreAndroid.initialize].
 * Public so the android-delegates artifact can install without sharing `internal` across modules.
 */
@Volatile
var kryptostoreAndroidFilesDir: Path? = null

internal actual fun resolveDataStoreFile(fileName: String): Path {
    val filesDir = kryptostoreAndroidFilesDir
        ?: error(
            "KryptostorePaths.file requires KryptostoreAndroid.initialize(context) " +
                "before use on Android (or use encryptedProtoDataStore / plainProtoDataStore delegates).",
        )
    val dir = filesDir / "datastore"
    FileSystem.SYSTEM.createDirectories(dir)
    return dir / fileName
}

/** Installs the Android application files directory used by [KryptostorePaths.file]. */
fun installKryptostoreAndroidFilesDir(directory: Path) {
    kryptostoreAndroidFilesDir = directory
}
