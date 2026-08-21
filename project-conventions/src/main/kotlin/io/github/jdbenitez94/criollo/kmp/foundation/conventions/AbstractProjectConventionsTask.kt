package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

/**
 * Shared inputs for sync/check of packaged convention files into a consumer repo.
 */
abstract class AbstractProjectConventionsTask : DefaultTask() {
    @get:Internal
    abstract val targetRoot: DirectoryProperty

    @get:Input
    abstract val kinds: ListProperty<String>

    internal fun selectedKinds(): List<ConventionFiles.Kind> = kinds.get().map { ConventionFiles.Kind.valueOf(it) }
}
