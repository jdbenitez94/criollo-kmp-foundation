package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.migrate

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

/** REQ-PKG-04 */
class ForbiddenDependenciesTest {
    @Test
    fun classpath_excludesSecurityCryptoAndDatastoreTink() {
        val classpath = System.getProperty("java.class.path").orEmpty()
        for (token in listOf("security-crypto", "datastore-tink")) {
            assertFalse(
                classpath.split(File.pathSeparator).any { it.contains(token) },
                "Forbidden dependency fragment on classpath: $token",
            )
        }
    }
}
