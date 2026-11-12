package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import io.github.jdbenitez94.criollo.kmp.foundation.testing.KryptostorePackagingAssertions
import org.junit.jupiter.api.Test

/** REQ-PKG-04, REQ-PKG-05 */
class ForbiddenDependenciesTest {
    @Test
    fun publicApiPackage_isFoundationKryptostore() {
        KryptostorePackagingAssertions.assertFoundationKryptostorePackage(
            EncryptedProtoSerializer::class.java.packageName,
        )
    }

    @Test
    fun jvmClasspath_excludesForbiddenArtifacts() {
        KryptostorePackagingAssertions.assertJvmClasspathExcludesForbiddenArtifacts()
    }
}
