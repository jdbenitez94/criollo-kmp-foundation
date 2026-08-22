package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** REQ-CRY-02, REQ-CRY-03 */
class CryptoRuntimeStateTest {
    @Test
    fun initialize_isIdempotentAndMutexSafe() = runTest {
        var rotateCalls = 0
        val stack = PlatformCryptoStack(
            cipher = IdentityCipher(),
            keyRotator = KeyRotator {
                rotateCalls++
                delay(20)
                false
            },
        )
        val runtime = CryptoRuntime(stack)

        val results = (1..8).map {
            async { runtime.initialize() }
        }.awaitAll()

        assertEquals(8, results.size)
        assertEquals(CryptoRuntimeState.Ready, runtime.state.value)
        assertEquals(1, rotateCalls)

        runtime.initialize()
        assertEquals(1, rotateCalls)
    }

    @Test
    fun initialize_failureSurfacesErrorNotReady() = runTest {
        val boom = IllegalStateException("keystore unavailable")
        val runtime = CryptoRuntime(
            PlatformCryptoStack(
                cipher = IdentityCipher(),
                keyRotator = KeyRotator { throw boom },
            ),
        )

        runtime.initialize()

        val error = assertIs<CryptoRuntimeState.Error>(runtime.state.value)
        assertEquals(boom, error.cause)
    }

    @Test
    fun defaultKryptoLog_isNoOp() = runTest {
        val runtime = CryptoRuntime(
            PlatformCryptoStack(
                cipher = IdentityCipher(),
                keyRotator = KeyRotator { throw IllegalStateException("fail") },
            ),
            log = KryptoLog.NoOp,
        )
        runtime.initialize()
        assertTrue(runtime.state.value is CryptoRuntimeState.Error)
    }
}

private class IdentityCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message
}

private fun KeyRotator(block: suspend () -> Boolean): KeyRotator = object : KeyRotator {
    override suspend fun rotateKeyIfNeeded(): Boolean = block()
}
