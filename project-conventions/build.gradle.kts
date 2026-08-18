import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.convention.ktlint)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.criollo.maven.publish)
}

group = ProjectConfig.group
version = ProjectConfig.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(gradleApi())
    testImplementation(gradleApi())
    testImplementation(kotlin("test"))
}

gradlePlugin {
    website.set(ProjectConfig.Publishing.repoUrl)
    vcsUrl.set("${ProjectConfig.Publishing.repoUrl}.git")
    plugins {
        create("projectConventions") {
            id = ProjectConfig.projectConventionsPluginId
            implementationClass =
                "io.github.jdbenitez94.criollo.kmp.foundation.conventions.ProjectConventionsPlugin"
            displayName = "Criollo project conventions"
            description =
                "Syncs Criollo .editorconfig and Detekt configs into consumer projects."
            tags.set(listOf("criollo", "editorconfig", "detekt", "ktlint", "conventions"))
        }
    }
}

tasks.processResources {
    from(rootProject.file(".editorconfig")) {
        into("criollo-kmp-foundation/conventions")
        rename { "editorconfig" }
    }
    from(rootProject.layout.projectDirectory.dir("config/detekt")) {
        include("detekt.yml", "detekt-v2.yml")
        into("criollo-kmp-foundation/conventions")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("compileKotlin") {
    mustRunAfter(tasks.named("clean"))
}
