package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import java.util.Properties

/**
 * Resolve a publish/signing property from Gradle (`-P` / `gradle.properties` / env),
 * falling back to root `local.properties` (gitignored secret store).
 */
fun Project.criolloProperty(name: String): String? {
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }?.let { return it }

    val localFile = rootProject.file("local.properties")
    if (!localFile.isFile) return null

    val props = Properties()
    localFile.reader(Charsets.UTF_8).use { props.load(it) }
    return props.getProperty(name)?.takeIf { it.isNotBlank() }
}
