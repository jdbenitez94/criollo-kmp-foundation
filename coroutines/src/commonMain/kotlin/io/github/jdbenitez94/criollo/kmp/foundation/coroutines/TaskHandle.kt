package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/**
 * Opaque handle to a task started by [TaskScope].
 */
class TaskHandle internal constructor(internal val job: Job) {
    val isActive: Boolean
        get() = job.isActive

    fun cancel() {
        job.cancel()
    }

    fun cancel(cause: CancellationException) {
        job.cancel(cause)
    }

    override fun equals(other: Any?): Boolean = other is TaskHandle && job === other.job

    override fun hashCode(): Int = job.hashCode()
}
