package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import android.content.Context

object AndroidCryptoContextHolder {
    private lateinit var context: Context

    val applicationContext: Context
        get() = context

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
