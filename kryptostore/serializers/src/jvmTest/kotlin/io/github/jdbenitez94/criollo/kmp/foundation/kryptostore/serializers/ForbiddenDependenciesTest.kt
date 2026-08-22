package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** REQ-PKG-04, REQ-PKG-05 */
class ForbiddenDependenciesTest {
    @Test
    fun publicApiPackage_isFoundationKryptostore() {
        val pkg = EncryptedProtoSerializer::class.java.packageName
        assertTrue(pkg.startsWith("io.github.jdbenitez94.criollo.kmp.foundation.kryptostore"))
        assertFalse(pkg.contains("composeApp"))
        assertFalse(pkg.contains("saveable"))
    }

    @Test
    fun jvmClasspath_excludesForbiddenArtifacts() {
        val classpath = System.getProperty("java.class.path").orEmpty()
        listOf("security-crypto", "datastore-tink", "jdbenitez94.saveable").forEach { token ->
            assertFalse(classpath.contains(token), "Forbidden token '$token' on classpath")
        }
    }
}
