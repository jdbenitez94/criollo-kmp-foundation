package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** REQ-PKG-04, REQ-PKG-05; IndexedDB resource sanity for REQ-STO-02. */
class CorePackagingTest {
    @Test
    fun publicApiPackage_isFoundationKryptostore() {
        val pkg = Class.forName(
            "io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.EncryptedProtoDataStoreKt",
        ).packageName
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

    @Test
    fun indexedDbResource_usesAppProtoDatabase() {
        val source = javaClass.classLoader.getResourceAsStream("indexeddb.ts")?.bufferedReader()?.readText()
        if (source != null) {
            assertTrue(source.contains("const PROTO_DB_NAME = 'app-proto'"))
            assertFalse(source.contains("localStorage"))
        }
    }
}
