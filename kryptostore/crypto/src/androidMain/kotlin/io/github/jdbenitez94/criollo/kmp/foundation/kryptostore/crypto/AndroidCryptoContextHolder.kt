package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import android.content.Context

/**
 * Holds the application [Context] required by Android's [createPlatformCryptoStack]
 * to access Keystore-backed `SharedPreferences`.
 *
 * Initialized automatically by `KryptostoreAndroid.initialize(context)` or directly via [init].
 */
object AndroidCryptoContextHolder {
    private lateinit var context: Context

    val applicationContext: Context
        get() = context

    fun init(context: Context) {
        this.context = context.applicationContext ?: context
    }
}
