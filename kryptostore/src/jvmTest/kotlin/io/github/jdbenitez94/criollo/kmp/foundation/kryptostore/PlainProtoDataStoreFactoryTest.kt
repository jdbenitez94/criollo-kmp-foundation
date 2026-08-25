package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.ProtoOkioSerializer
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue
import java.nio.file.Files as PlainTempFiles
import okio.Buffer as PlainProtoBuffer

/** REQ-STO-01 (file path) + plain typed StoreLocator API. */
class PlainProtoDataStoreFactoryTest {
    @Test
    fun platformLocator_createAndUpdateData_roundTrip_withoutCipher() = runTest {
        val dir = PlainTempFiles.createTempDirectory("kryptostore-plain").toFile()
        try {
            val path = dir.resolve("ui-settings.pb").absolutePath.toPath()
            val store = createPlainProtoDataStore(
                kSerializer = PlainSample.serializer(),
                defaultValue = PlainSample(),
                locator = StoreLocator.platform(producePath = { path }, name = "ui-settings"),
            )
            assertEquals(PlainSample(), store.data.first())
            store.updateData { PlainSample(theme = "dark") }
            assertEquals(PlainSample(theme = "dark"), store.data.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun producePath_convenience_matches_file_locator() = runTest {
        val dir = PlainTempFiles.createTempDirectory("kryptostore-plain-path").toFile()
        try {
            val path = dir.resolve("ui-settings.pb").absolutePath.toPath()
            val store = createPlainProtoDataStore(
                kSerializer = PlainSample.serializer(),
                defaultValue = PlainSample(),
                producePath = { path },
            )
            store.updateData { PlainSample(theme = "light") }
            assertEquals(PlainSample(theme = "light"), store.data.first())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun namedLocator_onJvm_failsClosed() {
        assertFails {
            createPlainProtoDataStore(
                kSerializer = PlainSample.serializer(),
                defaultValue = PlainSample(),
                locator = StoreLocator.named("ui-settings"),
            )
        }
    }

    @Test
    fun plainProtoSerializer_roundTripsWithoutEnvelope() = runTest {
        val serializer: ProtoOkioSerializer<PlainSample> = plainProtoSerializer(
            PlainSample.serializer(),
            PlainSample(),
        )
        val buffer = PlainProtoBuffer()
        serializer.writeTo(PlainSample(theme = "light"), buffer)
        val bytes = buffer.readByteArray()
        assertTrue(bytes.isNotEmpty())
        assertEquals(PlainSample(theme = "light"), serializer.readFrom(PlainProtoBuffer().write(bytes)))
    }

    @Test
    fun createPlain_storageOnly_usesDefaults() = runTest {
        val dir = PlainTempFiles.createTempDirectory("kryptostore-plain-storage").toFile()
        try {
            val path = dir.resolve("ui-settings.pb").absolutePath.toPath()
            val store = createPlainProtoDataStore(
                storage = androidx.datastore.core.okio.OkioStorage(
                    fileSystem = okio.FileSystem.SYSTEM,
                    serializer = plainProtoSerializer(PlainSample.serializer(), PlainSample()),
                    producePath = { path },
                ),
            )
            store.updateData { PlainSample(theme = "storage") }
            assertEquals(PlainSample(theme = "storage"), store.data.first())
        } finally {
            dir.deleteRecursively()
        }
    }
}

@Serializable
private data class PlainSample(val theme: String = "system")
