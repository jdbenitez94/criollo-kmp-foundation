package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Reads consumer project tree files outside this module's build dir.")
abstract class CheckProjectConventionsTask : DefaultTask() {
    @get:Internal
    abstract val targetRoot: DirectoryProperty

    @get:Input
    abstract val kinds: ListProperty<String>

    @TaskAction
    fun check() {
        val root = targetRoot.get().asFile
        val selected = kinds.get().map { ConventionFiles.Kind.valueOf(it) }
        val mismatches = mutableListOf<String>()

        selected.forEach { kind ->
            val target = ConventionFiles.targetFile(root, kind)
            val expected = ConventionFiles.read(kind)
            when {
                !target.isFile ->
                    mismatches += "${kind.displayName} is missing (run syncCriolloProjectConventions)"

                !target.readBytes().contentEquals(expected) ->
                    mismatches += "${kind.displayName} differs from Criollo foundation conventions"
            }
        }

        if (mismatches.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Criollo project conventions check failed:")
                    mismatches.forEach { appendLine("  - $it") }
                    append("Run ./gradlew syncCriolloProjectConventions to update.")
                },
            )
        }
    }
}
