package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.EnumSet

/**
 * Reduced-security fallback for Windows/Linux: master key bytes in a owner-only file under
 * `~/.kryptostore/<appId>/`. Prefer OS credential stores when available.
 */
internal class PosixSealedFileMasterKeyStore(private val appId: String) : JvmSecureMasterKeyStore {
    override fun readOrCreate(account: String): ByteArray {
        val baseDir = File(System.getProperty("user.home"), ".kryptostore/$appId/crypto").apply {
            mkdirs()
            runCatching {
                Files.setPosixFilePermissions(
                    toPath(),
                    EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    ),
                )
            }
        }
        val keyFile = File(baseDir, "$account.key")
        if (keyFile.exists() && keyFile.length() == 32L) {
            return keyFile.readBytes()
        }
        val generated = ByteArray(32).also { SecureRandom().nextBytes(it) }
        keyFile.writeBytes(generated)
        runCatching {
            Files.setPosixFilePermissions(
                keyFile.toPath(),
                EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                ),
            )
        }
        return generated
    }
}
