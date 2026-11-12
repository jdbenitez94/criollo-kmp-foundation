package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import io.github.jdbenitez94.criollo.kmp.foundation.testing.KryptostorePackagingAssertions
import org.junit.jupiter.api.Test

/** REQ-AND-04 / REQ-PKG-04 */
class ForbiddenDependenciesTest {
    @Test
    fun classpath_excludesSecurityCryptoAndDatastoreTink() {
        KryptostorePackagingAssertions.assertJvmClasspathExcludesForbiddenArtifacts(
            tokens = listOf("security-crypto", "datastore-tink"),
        )
    }
}
