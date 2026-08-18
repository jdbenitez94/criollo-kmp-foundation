package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class ProjectConventionsExtension {
    /** Directory that receives `.editorconfig` and Detekt YAML under `config/detekt/` (project dir by default). */
    abstract val targetRoot: DirectoryProperty

    /** Write/check `.editorconfig`. */
    abstract val includeEditorConfig: Property<Boolean>

    /** Write/check `config/detekt/detekt.yml` (Detekt 1). */
    abstract val includeDetektV1: Property<Boolean>

    /** Write/check `config/detekt/detekt-v2.yml` (Detekt 2). */
    abstract val includeDetektV2: Property<Boolean>

    /**
     * When true, `check` depends on `checkCriolloProjectConventions`.
     * Default false so applying the plugin does not fail builds before the first sync.
     */
    abstract val attachCheckToLifecycle: Property<Boolean>
}
