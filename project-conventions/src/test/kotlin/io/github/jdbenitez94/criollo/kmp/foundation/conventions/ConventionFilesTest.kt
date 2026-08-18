package io.github.jdbenitez94.criollo.kmp.foundation.conventions

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ConventionFilesTest {
    @Test
    fun packagedEditorconfigMatchesFoundationStylePolicy() {
        val text = ConventionFiles.read(ConventionFiles.Kind.EDITORCONFIG).decodeToString()
        assertContains(text, "ktlint_code_style = intellij_idea")
        assertContains(text, "max_line_length = 180")
        assertContains(text, "indent_size = 4")
        assertContains(text, "ij_kotlin_allow_trailing_comma = true")
    }

    @Test
    fun packagedDetektConfigsIncludeComposeSaverNaming() {
        val v1 = ConventionFiles.read(ConventionFiles.Kind.DETEKT_V1).decodeToString()
        val v2 = ConventionFiles.read(ConventionFiles.Kind.DETEKT_V2).decodeToString()
        assertContains(v1, "*Saver")
        assertContains(v2, "*Saver")
        assertContains(v1, "MatchingDeclarationName:")
        assertContains(v2, "MatchingDeclarationName:")
        assertTrue(v1.contains("active: false"))
        assertTrue(v2.contains("active: false"))
    }
}
