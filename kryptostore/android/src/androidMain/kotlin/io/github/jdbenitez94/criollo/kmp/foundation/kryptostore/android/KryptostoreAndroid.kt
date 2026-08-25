package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import android.content.Context
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.installKryptostoreAndroidFilesDir
import okio.Path.Companion.toPath

/**
 * One-time Android setup for [io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.KryptostorePaths]
 * when creating stores from commonMain (not required for Context property delegates).
 *
 * Call from `Application.onCreate` (or an early ContentProvider).
 */
object KryptostoreAndroid {
    fun initialize(context: Context) {
        val filesDir = context.applicationContext.filesDir.absolutePath.toPath()
        installKryptostoreAndroidFilesDir(filesDir)
    }
}
