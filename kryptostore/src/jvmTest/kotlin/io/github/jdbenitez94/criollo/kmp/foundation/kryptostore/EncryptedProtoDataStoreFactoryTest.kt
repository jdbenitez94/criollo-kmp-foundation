package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedProtoSerializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher as ProtoFactoryCipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions as ProtoFactoryOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator as ProtoFactoryLocator

/** REQ-STO-01, REQ-STO-05, REQ-STO-06 */
class EncryptedProtoDataStoreFactoryTest {
    @Test
    fun platformLocator_createAndUpdateData_roundTrip() = runTest {
        withTempPb("kryptostore-core") { path ->
            val store = createEncryptedProtoDataStore(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                locator = ProtoFactoryLocator.platform(producePath = { path }, name = "settings"),
                options = ProtoFactoryOptions().apply { storeName = "settings" },
            )
            assertEquals(Sample(), store.data.first())
            store.updateData { Sample(enabled = true) }
            assertEquals(Sample(enabled = true), store.data.first())
        }
    }

    @Test
    fun migrations_areInvoked() = runTest {
        withTempPb("kryptostore-mig") { path ->
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
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
                migrations = listOf(migration),
            )
            assertEquals(Sample(enabled = true), store.data.first())
            assertTrue(migrated)
        }
    }

    @Test
    fun namedLocator_onJvm_failsClosed() {
        assertFails {
            createEncryptedProtoDataStore(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                locator = ProtoFactoryLocator.named("settings"),
            )
        }
    }

    @Test
    fun encryptedProtoSerializer_buildsUsableSerializer() = runTest {
        val serializer: EncryptedProtoSerializer<Sample> = encryptedProtoSerializer(
            cipher = ProtoReverseCipher(),
            kSerializer = Sample.serializer(),
            defaultValue = Sample(),
            options = ProtoFactoryOptions().apply { storeName = "x" },
        )
        roundTripSerializer(serializer, Sample(enabled = true))
    }

    @Test
    fun encryptedProtoSerializer_omitsOptions_usesDefaults() = runTest {
        val serializer = encryptedProtoSerializer(
            cipher = ProtoReverseCipher(),
            kSerializer = Sample.serializer(),
            defaultValue = Sample(),
        )
        roundTripSerializer(serializer, Sample(enabled = true))
    }

    @Test
    fun createEncrypted_storageOnly_usesDefaults() = runTest {
        withTempPb("kryptostore-storage-only") { path ->
            val serializer = encryptedProtoSerializer(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
            )
            val store = createEncryptedProtoDataStore(
                storage = OkioStorage(
                    fileSystem = FileSystem.SYSTEM,
                    serializer = serializer,
                    producePath = { path },
                ),
            )
            assertEquals(Sample(), store.data.first())
            store.updateData { Sample(enabled = true) }
            assertEquals(Sample(enabled = true), store.data.first())
        }
    }

    @Test
    fun createEncrypted_producePath_omitsMigrations() = runTest {
        withTempPb("kryptostore-omit-mig") { path ->
            val store = createEncryptedProtoDataStore(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
            )
            assertEquals(Sample(), store.data.first())
        }
    }

    @Test
    fun createEncrypted_withRegistry_registersHandle() = runTest {
        withTempPb("kryptostore-registry") { path ->
            val registry = StoreRegistry()
            val store = createEncryptedProtoDataStore(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
                registry = registry,
            )
            store.updateData { Sample(enabled = true) }
            assertEquals(1, registry.size())
            registry.reEncryptAll()
            assertEquals(Sample(enabled = true), store.data.first())
        }
    }

    @Test
    fun createEncrypted_explicitCorruptionHandler() = runTest {
        withTempPb("kryptostore-corrupt-h") { path ->
            var handled = false
            val store = createEncryptedProtoDataStore(
                cipher = ProtoReverseCipher(),
                kSerializer = Sample.serializer(),
                defaultValue = Sample(),
                producePath = { path },
                corruptionHandler = ReplaceFileCorruptionHandler { _ ->
                    handled = true
                    Sample(enabled = true)
                },
            )
            assertEquals(Sample(), store.data.first())
            assertTrue(!handled)
        }
    }
}

private suspend fun roundTripSerializer(serializer: EncryptedProtoSerializer<Sample>, value: Sample) {
    val out = Buffer()
    serializer.writeTo(value, out)
    assertEquals(value, serializer.readFrom(Buffer().write(out.readByteArray())))
}

private suspend fun withTempPb(label: String, block: suspend (Path) -> Unit) {
    val dir = Files.createTempDirectory(label).toFile()
    try {
        block(dir.resolve("settings.pb").absolutePath.toPath())
    } finally {
        dir.deleteRecursively()
    }
}

@Serializable
private data class Sample(val enabled: Boolean = false)

private class ProtoReverseCipher : ProtoFactoryCipher {
    override suspend fun encrypt(message: ByteArray, associatedData: ByteArray?): ByteArray =
        message.copyOf().also { it.reverse() }

    override suspend fun decrypt(message: ByteArray, associatedData: ByteArray?): ByteArray =
        encrypt(message, associatedData)
}
