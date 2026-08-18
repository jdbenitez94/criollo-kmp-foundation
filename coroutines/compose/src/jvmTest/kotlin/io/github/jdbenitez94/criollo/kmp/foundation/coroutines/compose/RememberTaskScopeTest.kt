package io.github.jdbenitez94.criollo.kmp.foundation.coroutines.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskKey
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskPolicy
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskScope
import kotlinx.coroutines.delay
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class RememberTaskScopeTest {

    @Test
    fun rememberTaskScope_returnsScope() = runComposeUiTest {
        var scope: TaskScope? = null
        setContent {
            scope = rememberTaskScope()
        }
        waitForIdle()
        expectThat(scope).isNotNull()
    }

    @Test
    fun rememberTaskScope_cancelsTasksOnDispose() = runComposeUiTest {
        val visibility = mutableStateOf(true)
        val key = TaskKey.of("compose.dispose")
        var scopeHolder: TaskScope? = null

        setContent {
            if (visibility.value) {
                val tasks = rememberTaskScope()
                scopeHolder = tasks
                LaunchedEffect(tasks) {
                    tasks.launch(key, TaskPolicy.SkipIfActive) {
                        delay(60.seconds)
                    }
                }
            }
        }
        waitForIdle()
        val scope = scopeHolder
        expectThat(scope).isNotNull()
        expectThat(scope!!.isActive(key)).isTrue()

        visibility.value = false
        waitForIdle()
        expectThat(scope.isActive(key)).isFalse()
    }
}
