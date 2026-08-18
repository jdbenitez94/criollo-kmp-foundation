package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

/**
 * Outcome of [TaskScope.launch].
 */
sealed interface TaskLaunchResult {
    data class Started(val handle: TaskHandle) : TaskLaunchResult

    /** Returned when [TaskPolicy.SkipIfActive] prevented a duplicate launch. */
    data object Skipped : TaskLaunchResult
}
