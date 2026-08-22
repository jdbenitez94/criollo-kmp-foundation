import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Wires JUnit 5 (Jupiter) for JVM unit tests — aligned with saveable’s testing stack.
 *
 * - KMP: Jupiter on `jvmTest` / `androidHostTest` (when present) + platform launcher on `*RuntimeOnly`
 * - Kotlin JVM: `testImplementation` / `testRuntimeOnly`
 * - All [Test] tasks: [Test.useJUnitPlatform]
 *
 * Keep `kotlin.test` on `commonTest` for multiplatform sources; use `org.junit.jupiter` in `jvmTest`.
 */
class Junit5ConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val catalog = target.libs

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.configureEach {
                    if (name == "jvmTest" || name == "androidHostTest") {
                        dependencies {
                            // Catalog entries already carry the Jupiter version (BOM not needed here).
                            implementation(catalog.findLibrary("org-junit-jupiter").get())
                            implementation(catalog.findLibrary("org-junit-jupiter-params").get())
                        }
                    }
                }
            }
            target.afterEvaluate {
                addJunitPlatformLauncher(catalog, "jvmTestRuntimeOnly")
                addJunitPlatformLauncher(catalog, "androidHostTestRuntimeOnly")
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.dependencies {
                "testImplementation"(platform(catalog.findLibrary("org-junit-jupiter-bom").get()))
                "testImplementation"(catalog.findLibrary("org-junit-jupiter").get())
                "testImplementation"(catalog.findLibrary("org-junit-jupiter-params").get())
                "testRuntimeOnly"(catalog.findLibrary("org-junit-platform-launcher").get())
            }
        }

        target.tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }

    private fun Project.addJunitPlatformLauncher(catalog: VersionCatalog, configurationName: String) {
        val configuration = configurations.findByName(configurationName) ?: return
        dependencies.add(
            configuration.name,
            catalog.findLibrary("org-junit-platform-launcher").get(),
        )
    }
}
