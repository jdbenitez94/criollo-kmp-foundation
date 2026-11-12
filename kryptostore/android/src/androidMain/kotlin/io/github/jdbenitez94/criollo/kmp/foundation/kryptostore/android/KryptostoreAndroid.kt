package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import android.content.Context
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.AndroidCryptoContextHolder
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.installKryptostoreAndroidFilesDir
import okio.Path.Companion.toPath

/**
 * One-time Android setup for [AndroidCryptoContextHolder] and
 * [io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.KryptostorePaths]
 * when creating crypto stacks and stores from commonMain (not required for Context property delegates).
 *
 * Call from `Application.onCreate` (or an early ContentProvider).
 */
object KryptostoreAndroid {
    fun initialize(context: Context) {
        val appContext = context.applicationContext ?: context
        AndroidCryptoContextHolder.init(appContext)
        val filesDir = appContext.filesDir.absolutePath.toPath()
        installKryptostoreAndroidFilesDir(filesDir)
    }
}
