package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import java.util.Properties
import kotlin.reflect.KClass

/**
 * Resolve a property from Gradle (`-P` / `gradle.properties` / env),
 * falling back to root `local.properties` (gitignored secret store).
 *
 * Supported [T]: [String], [Boolean], [Int], [Long], [Double], [Float].
 *
 * @return parsed value, or `null` when missing / blank / unparsable
 */
inline fun <reified T : Any> Project.localProperty(name: String): T? {
    val raw = localPropertyValue(name) ?: return null
    return parseLocalProperty(raw, T::class)
}

/**
 * Like [localProperty], but returns [default] when missing / blank / unparsable.
 */
inline fun <reified T : Any> Project.localProperty(name: String, default: T): T = localProperty(name) ?: default

/** Raw lookup used by [localProperty]; blank values are treated as absent. */
fun Project.localPropertyValue(name: String): String? {
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }?.let { return it }

    val localFile = rootProject.file("local.properties")
    if (!localFile.isFile) return null

    val props = Properties()
    localFile.reader(Charsets.UTF_8).use { props.load(it) }
    return props.getProperty(name)?.takeIf { it.isNotBlank() }
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T : Any> parseLocalProperty(raw: String, type: KClass<T>): T? = when (type) {
    String::class -> raw as T
    Boolean::class -> raw.equals("true", ignoreCase = true) as T
    Int::class -> raw.toIntOrNull() as T?
    Long::class -> raw.toLongOrNull() as T?
    Double::class -> raw.toDoubleOrNull() as T?
    Float::class -> raw.toFloatOrNull() as T?
    else -> error(
        "Unsupported localProperty type ${type.simpleName}. " +
            "Supported: String, Boolean, Int, Long, Double, Float.",
    )
}
