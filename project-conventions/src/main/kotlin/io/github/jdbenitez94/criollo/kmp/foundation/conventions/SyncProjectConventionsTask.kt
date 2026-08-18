package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Writes into the consumer project tree outside this module's build dir.")
abstract class SyncProjectConventionsTask : DefaultTask() {
    @get:Internal
    abstract val targetRoot: DirectoryProperty

    @get:Input
    abstract val kinds: ListProperty<String>

    @TaskAction
    fun sync() {
        val root = targetRoot.get().asFile
        val selected = kinds.get().map { ConventionFiles.Kind.valueOf(it) }
        selected.forEach { kind ->
            val target = ConventionFiles.targetFile(root, kind)
            target.parentFile?.mkdirs()
            val bytes = ConventionFiles.read(kind)
            if (target.isFile && target.readBytes().contentEquals(bytes)) {
                logger.lifecycle("Up to date: ${kind.displayName}")
            } else {
                target.writeBytes(bytes)
                logger.lifecycle("Wrote ${kind.displayName}")
            }
        }
    }
}
