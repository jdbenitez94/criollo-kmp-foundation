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
        ivy {
            name = "Node.js"
            url = uri("https://nodejs.org/dist/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
        ivy {
            name = "Yarn"
            url = uri("https://github.com/yarnpkg/yarn/releases/download/")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.yarnpkg", "yarn")
            }
        }
        ivy {
            name = "Binaryen"
            url = uri("https://github.com/WebAssembly/binaryen/releases/download/")
            patternLayout {
                artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.github.webassembly", "binaryen")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Composite library resolution for buildscript classpath (pairs with beforeProject above).
// pluginManagement.includeBuild alone only covers plugin markers, not buildscript deps.
includeBuild("build-logic")

include(":bom")
include(":coroutines")
include(":coroutines:compose")
include(":coroutines:viewmodel")
include(":result")
include(":runtime")
include(":kryptostore")
include(":kryptostore:crypto")
include(":kryptostore:serializers")
include(":kryptostore:preferences")
include(":kryptostore:android")
include(":kryptostore:migrate-android")
include(":testing")
include(":project-conventions")

check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
    """
      Criollo KMP Foundation requires JDK 21+ but it is currently using JDK ${JavaVersion.current()}.
      Current Java Home: [${System.getProperty("java.home")}]
      Please update your JAVA_HOME or IDE Gradle JDK (Temurin 21) — required for -XX:+UseZGC.
    """.trimIndent()
}
