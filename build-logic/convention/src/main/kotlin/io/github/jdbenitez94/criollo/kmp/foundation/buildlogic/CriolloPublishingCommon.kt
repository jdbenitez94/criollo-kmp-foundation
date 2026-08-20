package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

fun Project.configureCriolloPublishing(artifactIdOverride: String? = null) {
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val versionString = version.toString().takeUnless { it == "unspecified" } ?: ProjectConfig.version
    require(versionString != "unspecified") {
        "Project $path must have a version (set ProjectConfig.version in build-logic)."
    }
    version = versionString
    group = ProjectConfig.group

    val baseArtifactId = artifactIdOverride ?: canonicalArtifactId(path, name)

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        afterEvaluate {
            val javadocJar =
                tasks.register<Jar>("emptyJavadocJar") {
                    archiveClassifier.set("javadoc")
                }
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().matching { it.name == "jvm" }.configureEach {
                    artifact(javadocJar)
                }
            }
        }
    }

    fun configurePublicationArtifactIds() {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                groupId = ProjectConfig.group
                // Plugin markers must keep Gradle's artifactId (= plugin id) for resolution.
                if (name.endsWith("PluginMarkerMaven")) {
                    return@configureEach
                }
                artifactId = when (name) {
                    "kotlinMultiplatform", "mavenJava", "pluginMaven" -> baseArtifactId
                    else -> "$baseArtifactId-$name"
                }
            }
        }
    }

    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name.set(baseArtifactId)
                description.set(pomDescriptionFor(baseArtifactId))
                url.set(ProjectConfig.Publishing.repoUrl)
                licenses {
                    license {
                        name.set(ProjectConfig.Publishing.licenseName)
                        url.set(ProjectConfig.Publishing.licenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(ProjectConfig.Publishing.developerId)
                        name.set(ProjectConfig.Publishing.developerName)
                    }
                }
                scm {
                    url.set(ProjectConfig.Publishing.repoUrl)
                    connection.set("scm:git:git://${ProjectConfig.Publishing.repoUrl.removePrefix("https://")}.git")
                    developerConnection.set(
                        "scm:git:ssh://${ProjectConfig.Publishing.repoUrl.removePrefix("https://")}.git",
                    )
                }
            }
        }

        repositories {
            maven {
                name = "mavenCentral"
                val releaseUrl = ProjectConfig.Publishing.mavenCentralReleaseUrl
                val snapshotUrl = ProjectConfig.Publishing.mavenCentralSnapshotUrl
                url = uri(if (versionString.endsWith("SNAPSHOT")) snapshotUrl else releaseUrl)

                credentials {
                    username = criolloProperty("mavenCentralUsername")
                    password = criolloProperty("mavenCentralPassword")
                }
            }
        }
    }

    configurePublicationArtifactIds()
    afterEvaluate { configurePublicationArtifactIds() }

    extensions.configure<SigningExtension> {
        val key = criolloProperty("signingInMemoryKey")
        val password = criolloProperty("signingInMemoryPassword")
        val keyId = criolloProperty("signingInMemoryKeyId")

        if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
            useInMemoryPgpKeys(keyId, key, password)
        }

        val isRequired = criolloProperty("signing.required")?.toBoolean() ?: false
        setRequired(isRequired)

        this@configureCriolloPublishing.extensions.configure<PublishingExtension> {
            sign(publications)
        }
    }
}

private fun canonicalArtifactId(projectPath: String, projectName: String): String = when (projectPath) {
    ":coroutines" -> ProjectConfig.Artifacts.coroutines
    ":coroutines:compose" -> ProjectConfig.Artifacts.coroutinesCompose
    ":coroutines:viewmodel" -> ProjectConfig.Artifacts.coroutinesViewmodel
    ":bom" -> ProjectConfig.Artifacts.bom
    ":project-conventions" -> ProjectConfig.Artifacts.projectConventions
    else -> projectName
}

private fun pomDescriptionFor(artifactId: String): String = when (artifactId) {
    ProjectConfig.Artifacts.bom ->
        "Bill of Materials for Criollo KMP Foundation artifacts."
    ProjectConfig.Artifacts.coroutines ->
        "Keyed TaskScope coroutine registry for Kotlin Multiplatform."
    ProjectConfig.Artifacts.coroutinesCompose ->
        "Compose rememberTaskScope() adapter for Criollo TaskScope."
    ProjectConfig.Artifacts.coroutinesViewmodel ->
        "ViewModel taskScope() property delegate for Criollo TaskScope."
    ProjectConfig.Artifacts.projectConventions ->
        "Gradle plugin that syncs Criollo .editorconfig and Detekt configs into consumer projects."
    else -> "Criollo KMP Foundation library module."
}
