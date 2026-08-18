package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Opt-in Gradle plugin for consumer repos that want Criollo shared style config:
 * `.editorconfig`, Detekt 1 (`detekt.yml`), and Detekt 2 (`detekt-v2.yml`).
 *
 * Does not apply Detekt/KtLint engines — only syncs/checks the shared config files.
 */
class ProjectConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create(
            "criolloProjectConventions",
            ProjectConventionsExtension::class.java,
        )
        extension.targetRoot.convention(target.layout.projectDirectory)
        extension.includeEditorConfig.convention(true)
        extension.includeDetektV1.convention(true)
        extension.includeDetektV2.convention(true)
        extension.attachCheckToLifecycle.convention(false)

        val kinds = target.provider {
            buildList {
                if (extension.includeEditorConfig.get()) add(ConventionFiles.Kind.EDITORCONFIG.name)
                if (extension.includeDetektV1.get()) add(ConventionFiles.Kind.DETEKT_V1.name)
                if (extension.includeDetektV2.get()) add(ConventionFiles.Kind.DETEKT_V2.name)
            }
        }

        val sync = target.tasks.register(
            "syncCriolloProjectConventions",
            SyncProjectConventionsTask::class.java,
        ) { task ->
            task.group = "criollo conventions"
            task.description =
                "Writes Criollo .editorconfig and Detekt config files into the project."
            task.targetRoot.set(extension.targetRoot)
            task.kinds.set(kinds)
        }

        val checkTask = target.tasks.register(
            "checkCriolloProjectConventions",
            CheckProjectConventionsTask::class.java,
        ) { task ->
            task.group = "verification"
            task.description =
                "Fails if .editorconfig / Detekt configs drift from Criollo foundation conventions."
            task.targetRoot.set(extension.targetRoot)
            task.kinds.set(kinds)
            task.mustRunAfter(sync)
        }

        target.afterEvaluate {
            if (extension.attachCheckToLifecycle.get()) {
                target.tasks.matching { it.name == "check" }.configureEach { checkLifecycle ->
                    checkLifecycle.dependsOn(checkTask)
                }
            }
        }
    }
}
