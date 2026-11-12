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
    configureJavaSourcesAndJavadocJars()
    configureEmptyKmpJavadocJar()
    configureMavenPublications(baseArtifactId, versionString)
    configurePublicationSigning()
}

private fun Project.configureJavaSourcesAndJavadocJars() {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

private fun Project.configureEmptyKmpJavadocJar() {
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
}

private fun Project.configureMavenPublications(baseArtifactId: String, versionString: String) {
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
                    username = localProperty<String>("mavenCentralUsername")
                    password = localProperty<String>("mavenCentralPassword")
                }
            }
            maven {
                name = "centralBundle"
                url = uri(rootProject.layout.buildDirectory.dir("central-bundle"))
            }
        }
    }

    configurePublicationArtifactIds()
    afterEvaluate { configurePublicationArtifactIds() }
}

private fun Project.configurePublicationSigning() {
    extensions.configure<SigningExtension> {
        val key = localProperty<String>("signingInMemoryKey")
        val password = localProperty<String>("signingInMemoryPassword")
        val keyId = localProperty<String>("signingInMemoryKeyId")

        if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
            useInMemoryPgpKeys(keyId, key, password)
        }

        setRequired(localProperty("signing.required", false))

        this@configurePublicationSigning.extensions.configure<PublishingExtension> {
            sign(publications)
        }
    }
}
