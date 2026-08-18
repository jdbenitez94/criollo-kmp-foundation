package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlin.time.Duration

/**
 * Launch policy for [TaskScope.launch].
 *
 * Choose based on whether duplicate work should be skipped, replaced, or debounced.
 */
sealed interface TaskPolicy {
    /** Ignore the launch while a task with the same [TaskKey] is already active. */
    data object SkipIfActive : TaskPolicy

    /** Cancel the active task for this key, then start a new one. */
    data object ReplaceActive : TaskPolicy

    /** Cancel active, wait [delay], then run. Built-in debounce for homogeneous streams. */
    data class Debounce(val delay: Duration) : TaskPolicy
}
