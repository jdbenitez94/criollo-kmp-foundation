package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** REQ-COR-01 */
class FailClosedCorruptionHandlerTest {
    private val fakeFileSystem = FakeFileSystem()
    private val path = "/tmp/settings.pb".toPath()

    @AfterEach
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

    @Test
    fun omitsFileSystemArg_usesPlatformFs() = runTest {
        val handler = failClosedCorruptionHandler<Unit>(producePath = { path })
        // Handler is created with default fileSystem lambda; invoking with missing file is a no-op quarantine.
        val original = CorruptionException("missing")
        val thrown = assertFailsWith<CorruptionException> {
            handler.handleCorruption(original)
        }
        assertTrue(thrown.message == original.message)
    }

    @Test
    fun nullFileSystem_noOp() {
        quarantineCorruptFile(path, fileSystem = null)
        // no throw
    }

    @Test
    fun missingFile_noOp() {
        quarantineCorruptFile(path, fakeFileSystem)
        assertFalse(fakeFileSystem.exists(path))
    }

    @Test
    fun missingFile_defaultFileSystemArg_noOp() {
        // Exercises default fileSystem= parameter and early-return when path is absent.
        quarantineCorruptFile("/tmp/definitely-missing-kryptostore.pb".toPath())
    }

    @Test
    fun replacesExistingCorruptFile() {
        fakeFileSystem.createDirectories(path.parent!!)
        fakeFileSystem.write(path) { writeUtf8("corrupt-bytes") }
        fakeFileSystem.write("/tmp/settings.pb.corrupt".toPath()) { writeUtf8("old-corrupt") }
        quarantineCorruptFile(path, fakeFileSystem)
        assertFalse(fakeFileSystem.exists(path))
        assertTrue(fakeFileSystem.exists("/tmp/settings.pb.corrupt".toPath()))
    }
}
