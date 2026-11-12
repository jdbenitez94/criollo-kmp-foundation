package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import io.github.jdbenitez94.criollo.kmp.foundation.testing.KryptostorePackagingAssertions
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** REQ-PKG-04, REQ-PKG-05; IndexedDB resource sanity for REQ-STO-02. */
class CorePackagingTest {
    @Test
    fun publicApiPackage_isFoundationKryptostore() {
        val pkg = Class.forName(
            "io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.EncryptedProtoDataStoreKt",
        ).packageName
        KryptostorePackagingAssertions.assertFoundationKryptostorePackage(pkg)
    }

    @Test
    fun jvmClasspath_excludesForbiddenArtifacts() {
        KryptostorePackagingAssertions.assertJvmClasspathExcludesForbiddenArtifacts()
    }

    @Test
    fun indexedDbResource_usesAppProtoDatabase() {
        val source = javaClass.classLoader.getResourceAsStream("indexeddb.js")?.bufferedReader()?.readText()
        if (source != null) {
            assertTrue(source.contains("const PROTO_DB_NAME = 'app-proto'"))
            assertFalse(source.contains("localStorage"))
        }
    }
}
