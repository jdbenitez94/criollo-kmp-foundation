import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.configureXcodeAvailability
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libsVersion
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.validateSemVer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "convention.root must be applied to the root project"
        }

        validateSemVer(ProjectConfig.version)
        target.configureXcodeAvailability()

        target.allprojects {
            group = ProjectConfig.group
            version = ProjectConfig.version
        }

        // Apply Detekt on root first so convention.ktlint can detect the Detekt 2 line
        // when choosing ktlint-engine vs ktlint-engine-legacy.
        target.pluginManager.apply("convention.detekt")
        target.pluginManager.apply("convention.kover.aggregation")
        target.pluginManager.apply("convention.dokka")
        val dokkaDeps = target.dependencies
        listOf(":coroutines", ":coroutines:compose", ":coroutines:viewmodel").forEach { path ->
            dokkaDeps.add("dokka", dokkaDeps.project(mapOf("path" to path)))
        }

        target.subprojects {
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                pluginManager.apply("convention.detekt")
                pluginManager.apply("convention.ktlint")
                configureExpectActualClasses()
            }
        }

        val installGitHooks = target.tasks.register("installGitHooks", InstallGitHooksTask::class.java) {
            group = "verification"
            description = "Points git core.hooksPath at gradle/hooks and marks hooks executable."
            rootDirProperty.set(target.layout.projectDirectory)
        }

        target.registerRootVerificationTasks(installGitHooks)
        target.registerRootAggregatorTasks()

        target.tasks.withType<Test>().configureEach {
            useJUnit()
        }

        target.libsVersion("kotlin")
    }

    private fun Project.configureExpectActualClasses() {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                targets.configureEach {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.add("-Xexpect-actual-classes")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Project.registerRootVerificationTasks(installGitHooks: TaskProvider<out Task>) {
        val ktlintCheck = tasks.register("ktlintCheck") {
            group = "verification"
            description = "Runs KtLint on all subprojects that apply convention.ktlint."
        }

        val qualityCheck = tasks.register("qualityCheck") {
            group = "verification"
            description = "Runs Detekt, KtLint, Kover verification, and project-conventions tests."
            dependsOn(installGitHooks, "koverVerify", ":project-conventions:test")
        }

        tasks.register("formatAndCheck") {
            group = "verification"
            description = "Auto-formats with KtLint and runs all quality checks."
            dependsOn(qualityCheck)
        }

        val rootDetekt = tasks.named("detekt")
        val formatAndCheck = tasks.named("formatAndCheck")
        qualityCheck.configure {
            dependsOn(ktlintCheck, rootDetekt)
        }

        subprojects {
            val subproject = this
            pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
                ktlintCheck.configure {
                    dependsOn(subproject.tasks.named("ktlintCheck"))
                }
                formatAndCheck.configure {
                    dependsOn(subproject.tasks.named("ktlintFormat"))
                }
            }
            pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
                rootDetekt.configure {
                    dependsOn(subproject.tasks.named("detekt"))
                }
            }
            pluginManager.withPlugin("dev.detekt") {
                rootDetekt.configure {
                    dependsOn(subproject.tasks.named("detekt"))
                }
            }
        }
    }

    private fun Project.registerRootAggregatorTasks() {
        tasks.register("jvmLibraryTests") {
            group = "verification"
            description = "Runs JVM unit tests for all library modules."
            dependsOn(
                ":coroutines:jvmTest",
                ":coroutines:compose:jvmTest",
                ":coroutines:viewmodel:jvmTest",
            )
        }

        tasks.register("publishToMavenLocal") {
            group = "publishing"
            description = "Publishes the BOM, library modules, and the project-conventions plugin locally."
            dependsOn(
                ":bom:publishToMavenLocal",
                ":coroutines:publishToMavenLocal",
                ":coroutines:compose:publishToMavenLocal",
                ":coroutines:viewmodel:publishToMavenLocal",
                ":project-conventions:publishToMavenLocal",
            )
        }
    }
}
