package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

/** Published Maven library modules included in root Kover merge + verify. */
val CRIOLLO_KOVER_PUBLISHED_LIBRARY_MODULES = listOf(
    ":coroutines",
    ":coroutines:compose",
    ":coroutines:viewmodel",
)

/** Modules that participate in the merged Kover report. */
val CRIOLLO_KOVER_MERGED_MODULE_PATHS: Set<String> =
    CRIOLLO_KOVER_PUBLISHED_LIBRARY_MODULES.toSet()
