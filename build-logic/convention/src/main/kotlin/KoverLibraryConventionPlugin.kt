import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.KoverExcludes.applyLibraryExcludes
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(KoverGradlePlugin::class.java)

        target.extensions.configure<KoverProjectExtension> {
            currentProject {
                if (target.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                    createVariant("kmp") {
                        add("jvm", optional = true)
                        add("android", optional = true)
                        add("js", optional = true)
                        add("wasmJs", optional = true)
                        add("iosSimulatorArm64", optional = true)
                        add("iosArm64", optional = true)
                    }
                }
            }
            reports {
                filters {
                    excludes {
                        applyLibraryExcludes()
                    }
                }
            }
        }
    }
}
