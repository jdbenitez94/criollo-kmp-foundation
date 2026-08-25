package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StoreLocatorTest {
    @Test
    fun platform_file_and_named_factories() {
        val path = "/tmp/settings.pb".toPath()
        val platform = StoreLocator.platform(producePath = { path }, name = "settings")
        val file = StoreLocator.file { path }
        val named = StoreLocator.named("settings")
        assertIs<StoreLocator.Platform>(platform)
        assertEquals(path, platform.producePath())
        assertEquals("settings", platform.name)
        assertIs<StoreLocator.File>(file)
        assertIs<StoreLocator.Named>(named)
    }

    @Test
    fun platform_and_named_reject_blank_name() {
        assertFailsWith<IllegalArgumentException> {
            StoreLocator.platform(producePath = { "/tmp/x".toPath() }, name = "   ")
        }
        assertFailsWith<IllegalArgumentException> {
            StoreLocator.named("   ")
        }
    }

    @Test
    fun resolve_platform_uses_file_when_usesFileStorage() {
        val path = "/tmp/settings.pb".toPath()
        val locator = StoreLocator.platform(producePath = { path }, name = "settings")
        val resolved = locator.resolve(
            onFile = { producePath -> "file:${producePath()}" },
            onNamed = { name -> "named:$name" },
        )
        if (kryptostoreUsesFileStorage) {
            assertTrue(resolved.startsWith("file:"), "expected file routing, got $resolved")
        } else {
            assertEquals("named:settings", resolved)
        }
    }

    @Test
    fun resolve_named_escape_hatch() {
        val resolved = StoreLocator.named("settings").resolve(
            onFile = { "file" },
            onNamed = { name -> "named:$name" },
        )
        assertEquals("named:settings", resolved)
    }

    @Test
    fun resolve_file_escape_hatch() {
        val path = "/tmp/settings.pb".toPath()
        val resolved = StoreLocator.file { path }.resolve(
            onFile = { producePath -> "file:${producePath()}" },
            onNamed = { "named" },
        )
        assertEquals("file:$path", resolved)
    }

    @Test
    fun resolve_platform_named_whenUsesFileStorageFalse() {
        val path = "/tmp/settings.pb".toPath()
        val resolved = StoreLocator.platform(producePath = { path }, name = "settings").resolve(
            usesFileStorage = false,
            onFile = { "file" },
            onNamed = { name -> "named:$name" },
        )
        assertEquals("named:settings", resolved)
    }
}
