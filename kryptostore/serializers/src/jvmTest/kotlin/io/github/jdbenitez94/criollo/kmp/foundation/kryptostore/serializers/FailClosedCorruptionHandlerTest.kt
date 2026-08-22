package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** REQ-COR-01 */
class FailClosedCorruptionHandlerTest {
    private val fakeFileSystem = FakeFileSystem()
    private val path = "/tmp/settings.pb".toPath()

    @AfterTest
    fun closeFakeFileSystem() {
        fakeFileSystem.close()
    }

    @Test
    fun handleCorruption_quarantinesFileAndRethrows() = runTest {
        fakeFileSystem.createDirectories(path.parent!!)
        fakeFileSystem.write(path) { writeUtf8("corrupt-bytes") }

        val handler = failClosedCorruptionHandler<Unit>(
            producePath = { path },
            fileSystem = { fakeFileSystem },
        )
        val original = CorruptionException("boom")

        val thrown = assertFailsWith<CorruptionException> {
            handler.handleCorruption(original)
        }
        assertTrue(thrown.message == original.message)
        assertFalse(fakeFileSystem.exists(path))
        assertTrue(fakeFileSystem.exists("/tmp/settings.pb.corrupt".toPath()))
    }
}
