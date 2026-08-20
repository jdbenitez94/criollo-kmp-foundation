package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

object ProjectConfig {
    const val version = "0.1.1" // x-release-please-version
    const val group = "io.github.jdbenitez94.criollo.kmp.foundation"

    /** Must match `group` in `build-logic/convention/build.gradle.kts`. */
    object BuildLogic {
        const val group = "io.github.jdbenitez94.criollo.kmp.foundation.buildlogic"
    }

    object Artifacts {
        const val bom = "bom"
        const val coroutines = "coroutines"
        const val coroutinesCompose = "coroutines-compose"
        const val coroutinesViewmodel = "coroutines-viewmodel"
        const val projectConventions = "project-conventions"
    }

    /** Published Gradle plugin id for consumer project style/config sync. */
    const val projectConventionsPluginId =
        "io.github.jdbenitez94.criollo.kmp.foundation.project-conventions"

    object Publishing {
        const val repoUrl = "https://github.com/jdbenitez94/criollo-kmp-foundation"
        const val licenseName = "MIT License"
        const val licenseUrl = "https://opensource.org/licenses/MIT"
        const val developerId = "jdbenitez94"
        const val developerName = "Joaquin Daniel Benitez"
        // Central Publisher Portal (OSSRH staging compatibility API; s01 OSSRH is EOL).
        const val mavenCentralReleaseUrl =
            "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
        const val mavenCentralSnapshotUrl =
            "https://central.sonatype.com/repository/maven-snapshots/"
    }

    object Android {
        const val compileSdk = 37
        const val minSdk = 24
    }

    object Namespaces {
        const val coroutines = "$group.coroutines"
        const val coroutinesCompose = "$group.coroutines.compose"
        const val coroutinesViewmodel = "$group.coroutines.viewmodel"
    }
}
