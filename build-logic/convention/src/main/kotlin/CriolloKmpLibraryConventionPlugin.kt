import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.KlibModuleNaming
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
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

            if (isXcodeAvailable()) {
                iosArm64()
                iosSimulatorArm64()
            }

            jvm()
            js { browser() }
            wasmJs { browser() }

            sourceSets.configureEach {
                if (name == "commonTest") {
                    dependencies {
                        implementation(libs.findLibrary("org-jetbrains-kotlin-test").get())
                    }
                }
            }
        }
    }
}
