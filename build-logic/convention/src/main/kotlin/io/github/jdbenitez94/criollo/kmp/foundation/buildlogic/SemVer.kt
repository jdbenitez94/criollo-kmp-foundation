package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.GradleException

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
