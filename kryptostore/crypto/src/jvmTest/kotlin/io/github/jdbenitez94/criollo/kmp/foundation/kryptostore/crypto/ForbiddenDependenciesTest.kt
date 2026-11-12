package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import io.github.jdbenitez94.criollo.kmp.foundation.testing.KryptostorePackagingAssertions
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * REQ-PKG-04, REQ-PKG-05 — classpath + package smoke checks.
 */
class ForbiddenDependenciesTest {
    @Test
    fun publicApiPackage_isFoundationKryptostore() {
        KryptostorePackagingAssertions.assertFoundationKryptostorePackage(Cipher::class.java.packageName)
    }

    @Test
    fun jvmClasspath_excludesForbiddenArtifacts() {
        KryptostorePackagingAssertions.assertJvmClasspathExcludesForbiddenArtifacts()
    }

    @Test
    fun encryptedBlobMagic_unchangedForWireCompat() {
        assertTrue(ENCRYPTED_BLOB_MAGIC == "SVBLENC1")
    }
}
