package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import java.security.SecureRandom

internal actual fun randomPlatformAesKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
