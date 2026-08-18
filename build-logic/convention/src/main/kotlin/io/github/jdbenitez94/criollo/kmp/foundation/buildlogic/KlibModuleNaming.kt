package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions

object KlibModuleNaming {
    val jsWasmTargets = setOf("js", "wasmJs")

    private const val KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY_ARG =
        "-Xklib-duplicated-unique-name-strategy=allow-all-with-warning"

    /**
     * Applies the KLIB duplicate `unique_name` strategy only on platform compilations.
     * Metadata compilations do not support this compiler flag.
     */
    fun configureDuplicatedUniqueNameStrategy(project: Project) {
        project.extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach {
                compilations.configureEach {
                    compileTaskProvider.configure {
                        if (!supportsKlibDuplicatedUniqueNameStrategy(name)) return@configure
                        compilerOptions {
                            freeCompilerArgs.add(KLIB_DUPLICATED_UNIQUE_NAME_STRATEGY_ARG)
                        }
                    }
                }
            }
        }
    }

    fun configureUniqueModuleName(project: Project) {
        val baseModuleName = project.uniqueKlibModuleName()
        project.extensions.configure<KotlinMultiplatformExtension> {
            targets.configureEach {
                if (name in jsWasmTargets) return@configureEach
                compilations.configureEach {
                    val compilationName = name
                    val uniqueModuleName = compilationModuleName(baseModuleName, compilationName)
                    compileTaskProvider.configure {
                        if (name.contains("Metadata", ignoreCase = true)) return@configure
                        compilerOptions {
                            when (this) {
                                is KotlinNativeCompilerOptions -> moduleName.set(uniqueModuleName)
                                is KotlinJvmCompilerOptions -> moduleName.set(uniqueModuleName)
                                else -> {
                                    val args = freeCompilerArgs.get().filterNot { it.startsWith("-module-name=") }
                                    freeCompilerArgs.set(args + "-module-name=$uniqueModuleName")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun compilationModuleName(baseModuleName: String, compilationName: String): String = if (compilationName == "main") {
        baseModuleName
    } else {
        "${baseModuleName}_$compilationName"
    }

    internal fun supportsKlibDuplicatedUniqueNameStrategy(compilationTaskName: String): Boolean = !compilationTaskName.contains("Metadata", ignoreCase = true)

    fun Project.uniqueKlibModuleName(): String = "criollo_" + path.removePrefix(":").replace(':', '_').replace('-', '_')
}
