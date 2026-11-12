package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KryptostorePathsTest {
    @Test
    fun file_resolves_under_home_datastore_on_jvm() {
        val path = KryptostorePaths.file("settings.pb")
        assertTrue(
            path.toString().endsWith("/.kryptostore/datastore/settings.pb") ||
                path.toString().endsWith("\\.kryptostore\\datastore\\settings.pb"),
        )
        assertEquals("settings.pb", path.name)
    }

    @Test
    fun file_rejects_blank_and_nested_names() {
        assertFailsWith<IllegalArgumentException> { KryptostorePaths.file("  ") }
        assertFailsWith<IllegalArgumentException> { KryptostorePaths.file("a/b.pb") }
    }

    @Test
    fun platformFileSystem_isAvailableOnJvm() {
        assertTrue(kryptostoreFileSystem != null)
        assertTrue(kryptostoreUsesFileStorage)
        val fs = requireKryptostoreFileSystem()
        assertTrue(requireKryptostoreFileSystem(fileSystem = fs) === fs)
    }

    @Test
    fun resolve_failsWhenUserHomeMissing() {
        val previous = System.getProperty("user.home")
        try {
            System.clearProperty("user.home")
            assertFailsWith<IllegalStateException> {
                KryptostorePaths.file("settings.pb")
            }
        } finally {
            if (previous != null) {
                System.setProperty("user.home", previous)
            }
        }
    }
}
