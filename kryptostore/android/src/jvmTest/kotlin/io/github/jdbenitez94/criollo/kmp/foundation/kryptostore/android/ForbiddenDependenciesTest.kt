package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

/** REQ-AND-04 / REQ-PKG-04 */
class ForbiddenDependenciesTest {
    @Test
    fun classpath_excludesSecurityCryptoAndDatastoreTink() {
        val classpath = System.getProperty("java.class.path").orEmpty()
        val forbidden = listOf("security-crypto", "datastore-tink")
        for (token in forbidden) {
            assertFalse(
                classpath.split(File.pathSeparator).any { it.contains(token) },
                "Forbidden dependency fragment on classpath: $token",
            )
        }
    }
}
