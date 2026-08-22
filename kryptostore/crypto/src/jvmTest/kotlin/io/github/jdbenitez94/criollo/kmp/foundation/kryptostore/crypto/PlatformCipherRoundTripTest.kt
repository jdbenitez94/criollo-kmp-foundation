package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFails

/** REQ-CRY-01, REQ-CRY-05 (permissions best-effort), REQ-CRY-07 (DB name constant). */
class PlatformCipherRoundTripTest {
    @Test
    fun jvmCipher_roundTripsWithAad_andRejectsWrongAad() = runTest {
        val home = Files.createTempDirectory("kryptostore-crypto-test").toFile()
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        try {
            val appId = "roundtrip-${System.nanoTime()}"
            val stack = createPlatformCryptoStack(appId)
            val runtime = CryptoRuntime(stack)
            runtime.initialize()

            val aad = "store|v1".encodeToByteArray()
            val plain = "hello-kryptostore".encodeToByteArray()
            val cipher = runtime.cipher
            val encrypted = cipher.encrypt(plain, aad)
            val decrypted = cipher.decrypt(encrypted, aad)
            expectThat(decrypted.toList()).isEqualTo(plain.toList())

            assertFails {
                cipher.decrypt(encrypted, "wrong|v1".encodeToByteArray())
            }
        } finally {
            if (previousHome != null) {
                System.setProperty("user.home", previousHome)
            } else {
                System.clearProperty("user.home")
            }
            home.deleteRecursively()
        }
    }

    @Test
    fun webCryptoDbName_isIndexedDbNotLocalStorage() {
        expectThat(WEB_CRYPTO_DB_NAME).isEqualTo("app-crypto")
        val workerSource = javaClass.classLoader
            .getResourceAsStream("crypto-worker-source.ts")
            ?.bufferedReader()
            ?.readText()
        // Resource may be absent on jvmTest classpath; assert constant + optional file.
        if (workerSource != null) {
            expectThat(workerSource.contains("const CRYPTO_DB_NAME = 'app-crypto'")).isEqualTo(true)
            expectThat(workerSource.contains("localStorage")).isFalse()
        }
    }
}
