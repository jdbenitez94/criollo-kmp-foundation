package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** REQ-STO — Platform routing uses Named when file storage is unavailable (JS/Wasm). */
class StoreLocatorJsTest {
    @Test
    fun resolve_platform_usesNamed_whenNotFileStorage() {
        assertFalse(kryptostoreUsesFileStorage)
        val locator = StoreLocator.platform(
            producePath = { "/unused".toPath() },
            name = "web-settings",
        )
        val resolved = locator.resolve(
            onFile = { "file" },
            onNamed = { name -> "named:$name" },
        )
        assertEquals("named:web-settings", resolved)
    }
}
