import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloResolvedVersion
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
    `java-library`
    alias(libs.plugins.convention.ktlint)
    alias(libs.plugins.convention.detekt)
    alias(libs.plugins.convention.dokka)
    alias(libs.plugins.convention.junit5)
    alias(libs.plugins.criollo.maven.publish)
}

group = ProjectConfig.group
version = criolloResolvedVersion()

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.org.jetbrains.kotlinx.coroutines.test)
    compileOnly(platform(libs.org.junit.jupiter.bom))
    compileOnly(libs.org.junit.jupiter)

    testImplementation(platform(libs.io.strikt.bom))
    testImplementation(libs.io.strikt.core)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks.named("compileKotlin") {
    mustRunAfter(tasks.named("clean"))
}
