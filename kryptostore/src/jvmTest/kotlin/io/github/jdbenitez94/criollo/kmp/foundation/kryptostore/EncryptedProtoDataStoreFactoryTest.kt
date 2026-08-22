package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedProtoSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/** REQ-STO-01, REQ-STO-05, REQ-STO-06 */
class EncryptedProtoDataStoreFactoryTest {
    @Test
    fun fileStore_createAndUpdateData_roundTrip() = runTest {
        val dir = Files.createTempDirectory("kryptostore-core").toFile()
        try {
            val path = dir.resolve("settings.pb").absolutePath.toPath()
            val store = createEncryptedProtoDataStore(
                cipher = ReversibleCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
                options = EncryptedStoreOptions().apply { storeName = "settings" },
            )
            assertEquals(Sample(), store.data.first())
            store.updateData { Sample(enabled = true) }
            assertEquals(Sample(enabled = true), store.data.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun migrations_areInvoked() = runTest {
        val dir = Files.createTempDirectory("kryptostore-mig").toFile()
        try {
            val path = dir.resolve("settings.pb").absolutePath.toPath()
            var migrated = false
            val migration = object : DataMigration<Sample> {
                override suspend fun shouldMigrate(currentData: Sample): Boolean = !migrated
                override suspend fun migrate(currentData: Sample): Sample {
                    migrated = true
                    return currentData.copy(enabled = true)
                }
                override suspend fun cleanUp() = Unit
            }
            val store = createEncryptedProtoDataStore(
                cipher = ReversibleCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
                migrations = listOf(migration),
            )
            assertEquals(Sample(enabled = true), store.data.first())
            assertTrue(migrated)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun indexedDbFactory_onJvm_failsClosed() {
        assertFails {
            createEncryptedProtoDataStoreIndexedDb(
                cipher = ReversibleCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                name = "settings",
            )
        }
    }

    @Test
    fun encryptedProtoSerializer_buildsUsableSerializer() = runTest {
        val serializer: EncryptedProtoSerializer<Sample> = encryptedProtoSerializer(
            cipher = ReversibleCipher(),
            kSerializer = Sample.serializer(),
            defaultValue = Sample(),
            options = EncryptedStoreOptions().apply { storeName = "x" },
        )
        val buffer = Buffer()
        serializer.writeTo(Sample(enabled = true), buffer)
        assertEquals(Sample(enabled = true), serializer.readFrom(Buffer().write(buffer.readByteArray())))
    }
}

@Serializable
private data class Sample(val enabled: Boolean = false)

private class ReversibleCipher : Cipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray = message.reversedArray()
}
