package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers

import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/** Android host coverage for KryptostorePaths / FileSystem actuals. */
class KryptostorePathsAndroidHostTest {
    @Test
    fun resolveDataStoreFile_requiresInitialize() {
        kryptostoreAndroidFilesDir = null
        val error = assertFails {
            KryptostorePaths.file("settings.pb")
        }
        assertTrue(error.message!!.contains("KryptostoreAndroid.initialize"))
    }

    @Test
    fun resolveDataStoreFile_afterInstall() {
        val dir = java.io.File.createTempFile("ks-ser-files", null).apply {
            delete()
            mkdirs()
        }
        try {
            installKryptostoreAndroidFilesDir(dir.absolutePath.toPath())
            val path = KryptostorePaths.file("settings.pb")
            assertEquals("settings.pb", path.name)
            assertTrue(path.toString().contains("datastore"))
        } finally {
            kryptostoreAndroidFilesDir = null
            dir.deleteRecursively()
        }
    }

    @Test
    fun platformFileSystem_isSystem() {
        assertTrue(kryptostoreFileSystem != null)
        assertTrue(kryptostoreUsesFileStorage)
    }
}
