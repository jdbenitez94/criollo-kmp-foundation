package io.github.jdbenitez94.criollo.kmp.foundation.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess

@OptIn(ExperimentalCoroutinesApi::class)
class MainTestDispatcherExtensionTest {

    @Nested
    @MainTestDispatcher
    inner class AnnotationInstallsMain {

        @Test
        fun main_isAvailable() {
            expectCatching { Dispatchers.Main.toString() }.isSuccess()
        }
    }

    @Nested
    inner class RegisterExtensionExposesDispatcher {

        @RegisterExtension
        @JvmField
        val main = MainTestDispatcherExtension { UnconfinedTestDispatcher() }

        @Test
        fun dispatcher_isExposedAndMainInstalled() {
            // Touch the lateinit property after BeforeEachCallback ran.
            expectThat(main.dispatcher.toString()).isEqualTo(main.dispatcher.toString())
            expectCatching { Dispatchers.Main.toString() }.isSuccess()
        }
    }
}
