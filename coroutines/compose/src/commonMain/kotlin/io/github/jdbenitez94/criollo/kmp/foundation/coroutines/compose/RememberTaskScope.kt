package io.github.jdbenitez94.criollo.kmp.foundation.coroutines.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskScope

/**
 * Remembers a [TaskScope] for the current Composable, cancelling all tasks on dispose.
 */
@Composable
fun rememberTaskScope(): TaskScope {
    val coroutineScope = rememberCoroutineScope()
    val taskScope = remember(coroutineScope) { TaskScope(coroutineScope) }
    DisposableEffect(taskScope) {
        onDispose {
            taskScope.cancelAll()
        }
    }
    return taskScope
}
