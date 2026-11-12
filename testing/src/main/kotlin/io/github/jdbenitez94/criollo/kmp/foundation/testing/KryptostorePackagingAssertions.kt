package io.github.jdbenitez94.criollo.kmp.foundation.testing

import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Shared classpath / package smoke checks for KryptoStore packaging requirements
 * (REQ-PKG-04). Kept here so module jvmTests do not clone the same asserts.
 */
object KryptostorePackagingAssertions {
    private val forbiddenClasspathTokens = listOf(
        "security-crypto",
        "datastore-tink",
    )

    fun assertFoundationKryptostorePackage(packageName: String) {
        assertTrue(
            packageName.startsWith("io.github.jdbenitez94.criollo.kmp.foundation.kryptostore"),
            "Unexpected package: $packageName",
        )
        assertFalse(packageName.contains("composeApp"), "Unexpected composeApp in $packageName")
    }

    fun assertJvmClasspathExcludesForbiddenArtifacts(
        classpath: String = System.getProperty("java.class.path").orEmpty(),
        tokens: List<String> = forbiddenClasspathTokens,
    ) {
        tokens.forEach { token ->
            assertFalse(
                classpath.contains(token),
                "Forbidden artifact token '$token' found on jvmTest classpath",
            )
        }
    }
}
