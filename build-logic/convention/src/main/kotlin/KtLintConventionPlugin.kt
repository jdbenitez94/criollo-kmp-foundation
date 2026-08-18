import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libsVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class KtLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        val usesDetektV2 = sequenceOf(target, target.rootProject).any { project ->
            project.pluginManager.hasPlugin("dev.detekt")
        }
        // Root owns the version catalog; subprojects may not expose it yet when RootPlugin
        // applies convention.ktlint from a parent `subprojects {}` block.
        val catalogProject = target.rootProject
        val ktlintEngineVersion =
            if (usesDetektV2) {
                catalogProject.libsVersion("ktlint-engine")
            } else {
                catalogProject.libsVersion("ktlint-engine-legacy")
            }

        target.extensions.configure<KtlintExtension> {
            android.set(false)
            ignoreFailures.set(false)
            enableExperimentalRules.set(false)
            version.set(ktlintEngineVersion)
            filter {
                exclude { element ->
                    val path = element.file.path
                    path.contains("/build/") ||
                        path.contains("\\build\\") ||
                        path.contains("/generated/") ||
                        path.contains("\\generated\\")
                }
            }
        }
    }
}
