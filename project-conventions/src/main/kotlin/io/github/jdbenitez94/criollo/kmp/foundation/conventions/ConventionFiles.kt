package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import java.io.File

/**
 * Canonical convention payloads packaged in this plugin JAR.
 *
 * Source of truth in the foundation monorepo: root `.editorconfig` and
 * `config/detekt/detekt.yml` / `detekt-v2.yml`.
 */
internal object ConventionFiles {
    const val RESOURCE_ROOT = "criollo-kmp-foundation/conventions"

    enum class Kind(val resourceName: String, val relativePath: String, val displayName: String) {
        EDITORCONFIG(
            resourceName = "editorconfig",
            relativePath = ".editorconfig",
            displayName = ".editorconfig",
        ),
        DETEKT_V1(
            resourceName = "detekt.yml",
            relativePath = "config/detekt/detekt.yml",
            displayName = "config/detekt/detekt.yml",
        ),
        DETEKT_V2(
            resourceName = "detekt-v2.yml",
            relativePath = "config/detekt/detekt-v2.yml",
            displayName = "config/detekt/detekt-v2.yml",
        ),
    }

    fun read(kind: Kind): ByteArray {
        val path = "$RESOURCE_ROOT/${kind.resourceName}"
        val stream = ConventionFiles::class.java.classLoader.getResourceAsStream(path)
            ?: error("Missing packaged convention resource: $path")
        return stream.use { it.readBytes() }
    }

    fun targetFile(rootDir: File, kind: Kind): File = File(rootDir, kind.relativePath)
}
