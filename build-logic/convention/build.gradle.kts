import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.org.jlleitschuh.gradle.ktlint)
    alias(libs.plugins.dev.detekt)
}

// Must match ProjectConfig.BuildLogic.group
group = "io.github.jdbenitez94.criollo.kmp.foundation.buildlogic"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

// Same policy as monorepo modules (Codacy analyzes this included build too).
detekt {
    buildUponDefaultConfig.set(true)
    autoCorrect.set(false)
    // Included-build root is build-logic/; parent is the monorepo root.
    val configDir = rootProject.projectDir.resolve("../config/detekt")
    config.setFrom(configDir.resolve("detekt-v2.yml"))
    baseline.set(configDir.resolve("baseline.xml"))
    source.setFrom("src/main/kotlin", "src/test/kotlin")
}

tasks.withType<KotlinCompile>().configureEach {
    mustRunAfter(tasks.named("clean"))
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.gradle.plugin)
    implementation(libs.com.android.tools.build.gradle)
    implementation(libs.org.jetbrains.kotlinx.kover.gradle.plugin)
    implementation(libs.io.gitlab.arturbosch.detekt.gradle.plugin)
    implementation(libs.dev.detekt.gradle.plugin)
    compileOnly(libs.org.jlleitschuh.gradle.ktlint.gradle.plugin)
    compileOnly(libs.org.jetbrains.dokka.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("root") {
            id = "convention.root"
            implementationClass = "RootPlugin"
        }
        register("criolloKmpLibrary") {
            id = "criollo.kmp-library"
            implementationClass = "CriolloKmpLibraryConventionPlugin"
        }
        register("criolloMavenPublish") {
            id = "criollo.maven-publish"
            implementationClass = "MavenPublishConventionPlugin"
        }
        register("ktlint") {
            id = "convention.ktlint"
            implementationClass = "KtLintConventionPlugin"
        }
        register("detekt") {
            id = "convention.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("dokka") {
            id = "convention.dokka"
            implementationClass = "DokkaConventionPlugin"
        }
        register("koverLibrary") {
            id = "convention.kover.library"
            implementationClass = "KoverLibraryConventionPlugin"
        }
        register("koverAggregation") {
            id = "convention.kover.aggregation"
            implementationClass = "KoverAggregationConventionPlugin"
        }
    }
}
