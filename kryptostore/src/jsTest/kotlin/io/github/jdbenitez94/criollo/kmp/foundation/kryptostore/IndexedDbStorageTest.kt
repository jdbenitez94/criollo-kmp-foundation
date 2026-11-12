package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher as IdbTestCipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions as IdbEncOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator as IdbLocator

/** REQ-STO-02 — web typed store uses IndexedDB via StoreLocator. */
@OptIn(ExperimentalUuidApi::class)
class IndexedDbStorageTest {
    @Test
    fun plainProto_platformLocator_persistsAcrossRecreate() = runTest {
        val name = "plain-proto-${Uuid.random()}"
        val locator = IdbLocator.platform(
            producePath = { error("unused on web") },
            name = name,
        )
        val first = createPlainProtoDataStore(
            kSerializer = WebSample.serializer(),
            defaultValue = WebSample(),
            locator = locator,
        )
        assertEquals(WebSample(), first.data.first())
        first.updateData { WebSample(theme = "dark") }
        assertEquals(WebSample(theme = "dark"), first.data.first())

        val second = createPlainProtoDataStore(
            kSerializer = WebSample.serializer(),
            defaultValue = WebSample(),
            locator = IdbLocator.named(name),
        )
        assertEquals(WebSample(theme = "dark"), second.data.first())
    }

    @Test
    fun encryptedProto_namedLocator_persistsAcrossRecreate() = runTest {
        val name = "enc-proto-${Uuid.random()}"
        val cipher = IndexedDbRotateCipher()
        val options = IdbEncOptions().apply { storeName = name }
        val first = createEncryptedProtoDataStore(
            cipher = cipher,
            kSerializer = WebSample.serializer(),
            defaultValue = WebSample(),
            locator = IdbLocator.named(name),
            options = options,
        )
        first.updateData { WebSample(theme = "secure") }
        assertEquals(WebSample(theme = "secure"), first.data.first())

        val second = createEncryptedProtoDataStore(
            cipher = cipher,
            kSerializer = WebSample.serializer(),
            defaultValue = WebSample(),
            locator = IdbLocator.named(name),
            options = options,
        )
        assertEquals(WebSample(theme = "secure"), second.data.first())
    }

    @Test
    fun producePath_encrypted_whenFileSystemNull_throws() {
        val error = kotlin.test.assertFails {
            createEncryptedProtoDataStore(
                cipher = IndexedDbRotateCipher(),
                kSerializer = WebSample.serializer(),
                defaultValue = WebSample(),
                producePath = { error("unused") },
            )
        }
        kotlin.test.assertTrue(error.message!!.contains("FileSystem is not available"))
    }

    @Test
    fun producePath_plain_whenFileSystemNull_throws() {
        val error = kotlin.test.assertFails {
            createPlainProtoDataStore(
                kSerializer = WebSample.serializer(),
                defaultValue = WebSample(),
                producePath = { error("unused") },
            )
        }
        kotlin.test.assertTrue(error.message!!.contains("FileSystem is not available"))
    }
}

@Serializable
private data class WebSample(val theme: String = "system")

/** Rotates bytes left by one for IndexedDB encrypted round-trips. */
private class IndexedDbRotateCipher : IdbTestCipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        if (message.isEmpty()) return message
        return ByteArray(message.size) { index ->
            message[(index + 1) % message.size]
        }
    }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray {
        if (message.isEmpty()) return message
        return ByteArray(message.size) { index ->
            message[(index + message.size - 1) % message.size]
        }
    }
}
