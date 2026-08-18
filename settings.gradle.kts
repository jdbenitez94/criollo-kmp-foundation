@file:Suppress("UnstableApiUsage")

rootProject.name = "criollo-kmp-foundation"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Retained: some classpath consumers still resolve build-logic via buildscript even with
// includeBuild("build-logic") below. Do not remove without verifying `./gradlew help`
// and convention plugin application.
gradle.beforeProject {
    buildscript {
        repositories {
            google {
                mavenContent {
                    includeGroupAndSubgroups("androidx")
                    includeGroupAndSubgroups("com.android")
                    includeGroupAndSubgroups("com.google")
                }
            }
            mavenCentral()
            gradlePluginPortal()
        }
        dependencies {
            classpath("io.github.jdbenitez94.criollo.kmp.foundation.buildlogic:convention")
        }
    }
}

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Composite library resolution for buildscript classpath (pairs with beforeProject above).
// pluginManagement.includeBuild alone only covers plugin markers, not buildscript deps.
includeBuild("build-logic")

include(":coroutines")
include(":coroutines:compose")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    """
      Criollo KMP Foundation requires JDK 21+ but it is currently using JDK ${JavaVersion.current()}.
      Current Java Home: [${System.getProperty("java.home")}]
      Please update your JAVA_HOME or IDE Gradle JDK (Temurin 21) — required for -XX:+UseZGC.
    """.trimIndent()
}
