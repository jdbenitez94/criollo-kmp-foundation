package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter

internal object KoverExcludes {
    fun KoverReportFilter.applyLibraryExcludes() {
        classes(
            "*_Generated*",
            $$"*$Companion",
        )
        annotatedBy(
            "kotlin.Deprecated",
        )
    }
}
