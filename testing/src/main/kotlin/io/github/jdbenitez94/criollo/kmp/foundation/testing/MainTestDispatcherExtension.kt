package io.github.jdbenitez94.criollo.kmp.foundation.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Installs [Dispatchers.Main] with a [TestDispatcher] for each test method.
 *
 * Use with `@ExtendWith(MainTestDispatcherExtension::class)` or [@MainTestDispatcher].
 * Access the active dispatcher via [dispatcher] when the extension is registered as a field:
 *
 * ```kotlin
 * @ExtendWith(MainTestDispatcherExtension::class)
 * class MyViewModelTest {
 *     @RegisterExtension
 *     @JvmField
 *     val main = MainTestDispatcherExtension()
 *
 *     @Test
 *     fun loads() = runTest(main.dispatcher) { … }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainTestDispatcherExtension(private val dispatcherFactory: () -> TestDispatcher = { StandardTestDispatcher() }) :
    BeforeEachCallback,
    AfterEachCallback {

    lateinit var dispatcher: TestDispatcher
        private set

    override fun beforeEach(context: ExtensionContext) {
        dispatcher = dispatcherFactory()
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}

/** Marks a test class to install [Dispatchers.Main] via [MainTestDispatcherExtension]. */
@ExtendWith(MainTestDispatcherExtension::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class MainTestDispatcher
