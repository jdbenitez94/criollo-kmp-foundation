import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.KlibModuleNaming
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloBooleanProperty
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloResolvedVersion
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.isXcodeAvailable
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CriolloKmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.group = ProjectConfig.group
        target.version = target.criolloResolvedVersion()

        with(target.pluginManager) {
            apply("convention.kover.library")
            apply("convention.ktlint")
            apply("convention.detekt")
            apply("convention.dokka")
            apply("criollo.maven-publish")
        }

        target.configureKmpTargets()
        // After KMP targets exist so jvmTest / androidHostTest source sets are present.
        target.pluginManager.apply("convention.junit5")
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            KlibModuleNaming.configureUniqueModuleName(target)
            KlibModuleNaming.configureDuplicatedUniqueNameStrategy(target)
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
    private fun Project.configureKmpTargets() {
        val androidNamespace = when (path) {
            ":coroutines" -> ProjectConfig.Namespaces.coroutines
            ":coroutines:compose" -> ProjectConfig.Namespaces.coroutinesCompose
            ":coroutines:viewmodel" -> ProjectConfig.Namespaces.coroutinesViewmodel
            ":result" -> ProjectConfig.Namespaces.result
            ":runtime" -> ProjectConfig.Namespaces.runtime
            ":kryptostore" -> ProjectConfig.Namespaces.kryptostore
            ":kryptostore:crypto" -> ProjectConfig.Namespaces.kryptostoreCrypto
            ":kryptostore:serializers" -> ProjectConfig.Namespaces.kryptostoreSerializers
            ":kryptostore:preferences" -> ProjectConfig.Namespaces.kryptostorePreferences
            ":kryptostore:android" -> ProjectConfig.Namespaces.kryptostoreAndroid
            ":kryptostore:migrate-android" -> ProjectConfig.Namespaces.kryptostoreMigrateAndroid
            else -> error("criollo.kmp-library applies only to known library modules (got $path)")
        }

        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()

            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                this.namespace = androidNamespace
                compileSdk = ProjectConfig.Android.compileSdk
                minSdk = ProjectConfig.Android.minSdk
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }

            // Apple targets need Xcode (klibs). Linux CI skips them; Maven publish must
            // run on macOS with -Pcriollo.requireAppleTargets=true so Central gets iOS.
            val requireAppleTargets = criolloBooleanProperty("criollo.requireAppleTargets")
            val xcodeAvailable = isXcodeAvailable()
            if (requireAppleTargets && !xcodeAvailable) {
                error(
                    "criollo.requireAppleTargets=true but Xcode is not available. " +
                        "Publish Apple klibs from a macOS runner (see docs/publishing.md).",
                )
            }
            if (requireAppleTargets || xcodeAvailable) {
                iosArm64()
                iosSimulatorArm64()
            }

            jvm()
            js { browser() }
            wasmJs { browser() }

            if (path in NON_WEB_HIERARCHY_MODULES) {
                val nonWebMain = sourceSets.create("nonWebMain") {
                    dependsOn(sourceSets.getByName("commonMain"))
                }
                sourceSets.getByName("androidMain").dependsOn(nonWebMain)
                sourceSets.getByName("jvmMain").dependsOn(nonWebMain)
                sourceSets.findByName("iosMain")?.dependsOn(nonWebMain)
            }

            val exposeCoroutinesApi = path in COROUTINES_API_MODULES
            val kryptostoreModule = path.startsWith(":kryptostore")
            val testingProject = rootProject.project(":testing")
            sourceSets.configureEach {
                when (name) {
                    "commonMain" -> {
                        dependencies {
                            val coroutines =
                                libs.findLibrary("org-jetbrains-kotlinx-coroutines-core").get()
                            if (exposeCoroutinesApi) {
                                api(coroutines)
                            } else {
                                implementation(coroutines)
                            }
                        }
                    }
                    "commonTest" -> {
                        dependencies {
                            implementation(libs.findLibrary("org-jetbrains-kotlin-test").get())
                            implementation(libs.findLibrary("org-jetbrains-kotlinx-coroutines-test").get())
                        }
                    }
                    "jvmTest", "androidHostTest" -> {
                        dependencies {
                            implementation(
                                project.dependencies.platform(libs.findLibrary("io-strikt-bom").get()),
                            )
                            implementation(libs.findLibrary("io-strikt-core").get())
                            if (kryptostoreModule) {
                                implementation(testingProject)
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        /** Modules that publish kotlinx-coroutines-core as an API dependency. */
        val COROUTINES_API_MODULES = setOf(":coroutines", ":result", ":runtime")

        /** nonWebMain intermediate source set (android/jvm/ios share native-ish storage). */
        val NON_WEB_HIERARCHY_MODULES = setOf(":kryptostore", ":kryptostore:preferences")
    }
}
