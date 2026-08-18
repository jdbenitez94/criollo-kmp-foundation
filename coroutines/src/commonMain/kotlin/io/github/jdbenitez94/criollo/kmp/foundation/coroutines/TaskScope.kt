package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Registry of keyed coroutine tasks scoped to a parent [CoroutineScope].
 *
 * Use one instance per lifecycle owner (ViewModel via `taskScope()`, Composable via `rememberTaskScope()`).
 *
 * Implementations must treat Idle/Running resets as identity-guarded: when a job completes, the key
 * returns to [TaskJobState.Idle] only if that same [Job] instance is still registered. Concurrent
 * [launch]/[cancel]/ and completion callbacks are safe from any thread.
 */
interface TaskScope {
    fun launch(key: TaskKey, policy: TaskPolicy = TaskPolicy.SkipIfActive, block: suspend CoroutineScope.() -> Unit): TaskLaunchResult

    fun cancel(key: TaskKey)

    fun cancelAll()

    fun isActive(key: TaskKey): Boolean
}

/** Creates a [TaskScope] bound to [coroutineScope]. */
fun TaskScope(coroutineScope: CoroutineScope): TaskScope = DefaultTaskScope(coroutineScope)

internal fun removeJobIfCurrent(jobs: MutableMap<TaskKey, Job>, key: TaskKey, completedJob: Job) {
    if (jobs[key] === completedJob) {
        jobs.remove(key)
    }
}
