package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Validates that a version string follows Semantic Versioning (SemVer).
 *
 * Expected format: `MAJOR.MINOR.PATCH[-prerelease][+build]`
 *
 * @throws GradleException if the format is invalid.
 */
fun validateSemVer(version: String) {
    val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+(-[\\w\\-.]+)?(\\+[\\w\\-.]+)?$")
    if (!semverPattern.matches(version)) {
        throw GradleException(
            "Invalid SemVer format: $version. Expected format: MAJOR.MINOR.PATCH[-prerelease][+build]",
        )
    }
}

/**
 * Effective publish/build version: `-Pcriollo.version=…` overrides [ProjectConfig.version]
 * (used for Central Portal SNAPSHOT publishes from `dev`).
 */
fun Project.criolloResolvedVersion(): String {
    val override = criolloProperty("criollo.version")
    return override?.also { validateSemVer(it) } ?: ProjectConfig.version
}

/**
 * Next patch SNAPSHOT for [releaseVersion] (e.g. `0.1.4` → `0.1.5-SNAPSHOT`).
 * Ignores any existing pre-release / build metadata on the base.
 */
fun nextPatchSnapshotVersion(releaseVersion: String): String {
    validateSemVer(releaseVersion)
    val core = releaseVersion.substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    require(parts.size == 3) { "Expected MAJOR.MINOR.PATCH, got: $releaseVersion" }
    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()
    return "$major.$minor.${patch + 1}-SNAPSHOT"
}
