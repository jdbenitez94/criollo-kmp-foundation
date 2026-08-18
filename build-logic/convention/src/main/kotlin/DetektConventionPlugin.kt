@file:Suppress("UnstableApiUsage")

import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import dev.detekt.gradle.DetektCreateBaselineTask as DetektCreateBaselineTaskV2
import dev.detekt.gradle.extensions.DetektExtension as DetektExtensionV2
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask as DetektCreateBaselineTaskV1
import io.gitlab.arturbosch.detekt.extensions.DetektExtension as DetektExtensionV1

/**
 * Applies Detekt with dual-stack support:
 * - Default: Detekt 2 (`dev.detekt`) + [config/detekt/detekt-v2.yml]
 * - Fallback: Detekt 1 (`io.gitlab.arturbosch.detekt`) if that plugin is already applied
 *
 * Ready for Detekt 2 stable: keep v1 config/path until consumers migrate fully.
 */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        when {
            target.pluginManager.hasPlugin(DETEKT_V2_PLUGIN) -> configureDetektV2(target)
            target.pluginManager.hasPlugin(DETEKT_V1_PLUGIN) -> configureDetektV1(target)
            else -> {
                target.apply(plugin = DETEKT_V2_PLUGIN)
                configureDetektV2(target)
            }
        }
    }

    private fun configureDetektV2(target: Project) {
        // Formatting stays with convention.ktlint (not detekt-rules-ktlint-wrapper) to avoid
        // dual enforcement against a separate KtLint check with different wrapping defaults.
        target.extensions.configure<DetektExtensionV2> {
            buildUponDefaultConfig.set(true)
            autoCorrect.set(false)
            val configDir = target.rootProject.file("config/detekt")
            config.setFrom(configDir.resolve("detekt-v2.yml"))
            baseline.set(configDir.resolve("baseline.xml"))
            source.setFrom(detektSourceFiles(target))
        }

        registerBaselineTaskIfRoot(target, configFile = "detekt-v2.yml")
    }

    private fun configureDetektV1(target: Project) {
        val formatting = target.rootProject.libs
            .findLibrary("io-gitlab-arturbosch-detekt-formatting")
            .orElseThrow { IllegalArgumentException("Missing detekt-formatting library") }
        target.dependencies.add("detektPlugins", formatting)

        target.extensions.configure<DetektExtensionV1> {
            buildUponDefaultConfig = true
            allRules = false
            autoCorrect = false
            val configDir = target.rootProject.file("config/detekt")
            config.setFrom(configDir.resolve("detekt.yml"))
            baseline = configDir.resolve("baseline.xml")
            source.setFrom(detektSourceFiles(target))
        }

        registerBaselineTaskIfRoot(target, configFile = "detekt.yml")
    }

    /**
     * Hand-written Kotlin only. Omitting `build/` keeps generated trees out of Detekt
     * (stronger than YAML excludes alone).
     */
    private fun detektSourceFiles(target: Project) = target.files(
        "src/commonMain/kotlin",
        "src/commonTest/kotlin",
        "src/androidMain/kotlin",
        "src/androidHostTest/kotlin",
        "src/iosMain/kotlin",
        "src/jvmMain/kotlin",
        "src/jvmTest/kotlin",
        "src/jsMain/kotlin",
        "src/jsTest/kotlin",
        "src/wasmJsMain/kotlin",
        "src/wasmJsTest/kotlin",
        "src/main/kotlin",
        "src/test/kotlin",
    )

    private fun registerBaselineTaskIfRoot(target: Project, configFile: String) {
        if (target != target.rootProject) return
        if (target.pluginManager.hasPlugin(DETEKT_V2_PLUGIN)) {
            target.tasks.register<DetektCreateBaselineTaskV2>("detektProjectBaseline") {
                group = "verification"
                description =
                    "Regenerates the shared baseline in config/detekt/baseline.xml for the entire repository."
                buildUponDefaultConfig.set(true)
                ignoreFailures.set(true)
                parallel.set(true)
                setSource(target.files(target.rootDir))
                config.setFrom(target.files("${target.rootDir}/config/detekt/$configFile"))
                baseline.set(target.layout.projectDirectory.file("config/detekt/baseline.xml"))
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**", "**/generated/**")
            }
        } else if (target.pluginManager.hasPlugin(DETEKT_V1_PLUGIN)) {
            target.tasks.register<DetektCreateBaselineTaskV1>("detektProjectBaseline") {
                group = "verification"
                description =
                    "Regenerates the shared baseline in config/detekt/baseline.xml for the entire repository."
                buildUponDefaultConfig.set(true)
                ignoreFailures.set(true)
                parallel.set(true)
                setSource(target.files(target.rootDir))
                config.setFrom(target.files("${target.rootDir}/config/detekt/$configFile"))
                baseline.set(target.layout.projectDirectory.file("config/detekt/baseline.xml"))
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**", "**/generated/**")
            }
        }
    }

    private companion object {
        const val DETEKT_V1_PLUGIN = "io.gitlab.arturbosch.detekt"
        const val DETEKT_V2_PLUGIN = "dev.detekt"
    }
}
