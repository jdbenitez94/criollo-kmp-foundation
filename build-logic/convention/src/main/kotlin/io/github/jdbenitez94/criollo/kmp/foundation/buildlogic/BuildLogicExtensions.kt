package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.libsVersion(alias: String): String = libs
    .findVersion(alias)
    .orElseThrow {
        IllegalArgumentException("Missing libs.versions.$alias")
    }.requiredVersion
