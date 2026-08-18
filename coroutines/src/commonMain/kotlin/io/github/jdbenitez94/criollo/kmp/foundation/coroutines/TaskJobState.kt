package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TaskJobState {
    Idle,
    Running,
}

fun TaskScope.taskState(key: TaskKey): StateFlow<TaskJobState> = (this as? StatefulTaskScope)?.taskState(key)
    ?: error("taskState requires TaskScope created via TaskScope(coroutineScope)")

internal interface StatefulTaskScope : TaskScope {
    fun taskState(key: TaskKey): StateFlow<TaskJobState>
}

/**
 * Default [TaskScope] implementation.
 *
 * ## Threading contract
 * All registry mutations (`jobs` / `jobStates`) are guarded by [lock]. Callers may invoke
 * [launch]/[cancel]/[isActive] from any thread; the Idle/Running state for a key is only reset
 * when the completing [Job] is still the registered job for that key (identity guard), so a
 * replacement under [TaskPolicy.ReplaceActive] never briefly reports [TaskJobState.Idle].
 */
internal class DefaultTaskScope(private val parentScope: CoroutineScope) : StatefulTaskScope {

    private val lock = TaskScopeLock()
    private val jobs = mutableMapOf<TaskKey, Job>()
    private val jobStates = mutableMapOf<TaskKey, MutableStateFlow<TaskJobState>>()

    override fun taskState(key: TaskKey): StateFlow<TaskJobState> = lock.withLock {
        jobStates.getOrPut(key) { MutableStateFlow(TaskJobState.Idle) }.asStateFlow()
    }

    override fun launch(key: TaskKey, policy: TaskPolicy, block: suspend CoroutineScope.() -> Unit): TaskLaunchResult {
        val started = lock.withLock {
            val activeJob = jobs[key]
            if (activeJob?.isActive == true) {
                when (policy) {
                    TaskPolicy.SkipIfActive -> return@withLock null
                    TaskPolicy.ReplaceActive -> activeJob.cancel()
                    is TaskPolicy.Debounce -> activeJob.cancel()
                }
            }

            val state = jobStates.getOrPut(key) { MutableStateFlow(TaskJobState.Idle) }
            val job = parentScope.launch(start = CoroutineStart.LAZY) {
                when (policy) {
                    TaskPolicy.SkipIfActive, TaskPolicy.ReplaceActive -> Unit
                    is TaskPolicy.Debounce -> delay(policy.delay)
                }
                state.value = TaskJobState.Running
                block()
            }

            jobs[key] = job
            job.invokeOnCompletion {
                lock.withLock {
                    if (jobs[key] === job) {
                        jobs.remove(key)
                        state.value = TaskJobState.Idle
                    }
                }
            }
            job.start()
            job
        }
        return if (started == null) {
            TaskLaunchResult.Skipped
        } else {
            TaskLaunchResult.Started(TaskHandle(started))
        }
    }

    override fun cancel(key: TaskKey) {
        lock.withLock {
            jobs.remove(key)?.cancel()
            jobStates[key]?.value = TaskJobState.Idle
        }
    }

    override fun cancelAll() {
        lock.withLock {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
            jobStates.values.forEach { it.value = TaskJobState.Idle }
        }
    }

    override fun isActive(key: TaskKey): Boolean = lock.withLock {
        jobs[key]?.isActive == true
    }
}
