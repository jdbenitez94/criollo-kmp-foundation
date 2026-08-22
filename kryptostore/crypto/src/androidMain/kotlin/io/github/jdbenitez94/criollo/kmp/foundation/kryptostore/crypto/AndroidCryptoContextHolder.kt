package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import android.content.Context

object AndroidCryptoContextHolder {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
