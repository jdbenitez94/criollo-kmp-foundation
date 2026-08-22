import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.configureXcodeAvailability
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloBooleanProperty
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloResolvedVersion
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libsVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class RootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "convention.root must be applied to the root project"
        }

        val resolvedVersion = target.criolloResolvedVersion()
        target.configureXcodeAvailability()

        target.allprojects {
            group = ProjectConfig.group
            version = resolvedVersion
        }

        // Apply Detekt on root first so convention.ktlint can detect the Detekt 2 line
        // when choosing ktlint-engine vs ktlint-engine-legacy.
        target.pluginManager.apply("convention.detekt")
        target.pluginManager.apply("convention.kover.aggregation")
        target.pluginManager.apply("convention.dokka")
        val dokkaDeps = target.dependencies
        listOf(
            ":coroutines",
            ":coroutines:compose",
            ":coroutines:viewmodel",
            ":kryptostore",
            ":kryptostore:crypto",
            ":kryptostore:serializers",
            ":kryptostore:preferences",
            ":kryptostore:android",
            ":kryptostore:migrate-android",
        ).forEach { path ->
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
        target.registerLocalCloudParityTask()

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
        val buildLogicDetekt = gradle.includedBuild("build-logic").task(":convention:detekt")
        qualityCheck.configure {
            dependsOn(ktlintCheck, rootDetekt, buildLogicDetekt)
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

    private fun Project.registerLocalCloudParityTask() {
        val coverageEnabled = criolloBooleanProperty("localCloudParity.coverage")
        val localCloudParity = tasks.register<LocalCloudParityTask>("localCloudParity") {
            group = "verification"
            description =
                "Optional local cloud parity: markdownlint + jscpd; with -PlocalCloudParity.coverage=true " +
                    "also runs tests + koverXmlReport and best-effort Codecov/Codacy uploads. " +
                    "Complexity: use detekt / qualityCheck."
            rootDirProperty.set(layout.projectDirectory)
            codecovSlug.set("jdbenitez94/criollo-kmp-foundation")
            runCoverageUploads.set(coverageEnabled)
            failOnMarkdownlint.set(true)
            failOnJscpd.set(true)
            koverReportFile.set(layout.buildDirectory.file("reports/kover/report.xml"))
        }
        if (coverageEnabled) {
            localCloudParity.configure {
                dependsOn("jvmLibraryTests", "koverXmlReport")
            }
        }

        tasks.register("preparePullRequest") {
            group = "verification"
            description =
                "Run before opening a PR (localCloudParity). Used by gradle/hooks/pre-pr."
            dependsOn(localCloudParity)
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
                ":kryptostore:jvmTest",
                ":kryptostore:crypto:jvmTest",
                ":kryptostore:serializers:jvmTest",
                ":kryptostore:preferences:jvmTest",
                ":kryptostore:android:jvmTest",
                ":kryptostore:migrate-android:jvmTest",
            )
        }

        tasks.register("checkKryptostoreAbi") {
            group = "verification"
            description = "Checks kryptostore JVM ABI dumps via binary-compatibility-validator (REQ-HRD-02)."
            dependsOn(
                ":kryptostore:apiCheck",
                ":kryptostore:crypto:apiCheck",
                ":kryptostore:serializers:apiCheck",
                ":kryptostore:preferences:apiCheck",
                ":kryptostore:android:apiCheck",
                ":kryptostore:migrate-android:apiCheck",
            )
        }

        tasks.register("dumpKryptostoreAbi") {
            group = "verification"
            description = "Updates kryptostore JVM ABI dumps (run separately from check)."
            dependsOn(
                ":kryptostore:apiDump",
                ":kryptostore:crypto:apiDump",
                ":kryptostore:serializers:apiDump",
                ":kryptostore:preferences:apiDump",
                ":kryptostore:android:apiDump",
                ":kryptostore:migrate-android:apiDump",
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
                ":kryptostore:publishToMavenLocal",
                ":kryptostore:crypto:publishToMavenLocal",
                ":kryptostore:serializers:publishToMavenLocal",
                ":kryptostore:preferences:publishToMavenLocal",
                ":kryptostore:android:publishToMavenLocal",
                ":kryptostore:migrate-android:publishToMavenLocal",
                ":project-conventions:publishToMavenLocal",
            )
        }
    }
}
