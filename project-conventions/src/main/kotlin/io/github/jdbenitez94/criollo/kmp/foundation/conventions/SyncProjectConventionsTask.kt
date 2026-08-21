package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Writes into the consumer project tree outside this module's build dir.")
abstract class SyncProjectConventionsTask : AbstractProjectConventionsTask() {
    @TaskAction
    fun sync() {
        val root = targetRoot.get().asFile
        selectedKinds().forEach { kind ->
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
