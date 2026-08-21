@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class TaskScopeTest {

    private val testKey = TaskKey.of("test.task")

    @Test
    fun skipIfActive_returnsSkipped() = runTest {
        val scope = TaskScope(backgroundScope)
        var executions = 0

        val first = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            executions++
        }
        val second = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            executions++
        }

        runCurrent()
        expectThat(first).isA<TaskLaunchResult.Started>()
        expectThat(second).isEqualTo(TaskLaunchResult.Skipped)
        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun replaceActive_cancelsPrevious() = runTest {
        val scope = TaskScope(backgroundScope)
        var lastCompleted = 0

        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            try {
                delay(1_000)
                lastCompleted = 1
            } catch (_: CancellationException) {
            }
        }
        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            lastCompleted = 2
        }

        advanceTimeBy(1_500)
        runCurrent()
        expectThat(lastCompleted).isEqualTo(2)
    }

    @Test
    fun replaceActive_neverReportsIdleWhileReplacementRuns() = runTest {
        val scope = TaskScope(backgroundScope)
        val state = scope.taskState(testKey)
        val observed = mutableListOf<TaskJobState>()

        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            try {
                delay(1_000)
            } catch (_: CancellationException) {
            }
        }
        runCurrent()
        expectThat(state.value).isEqualTo(TaskJobState.Running)

        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            delay(1_000)
        }
        // Drain the cancelled job's completion callback before the replacement starts.
        runCurrent()
        observed += state.value
        expectThat(observed.toList()).isEqualTo(listOf(TaskJobState.Running))
        expectThat(scope.isActive(testKey)).isTrue()
    }

    @Test
    fun debounce_delaysAndCoalesces() = runTest {
        val scope = TaskScope(backgroundScope)
        var executions = 0
        var lastValue = 0

        repeat(5) { index ->
            scope.launch(testKey, TaskPolicy.Debounce(300.milliseconds)) {
                lastValue = index
                executions++
            }
            advanceTimeBy(50)
        }

        advanceTimeBy(300)
        runCurrent()
        expectThat(executions).isEqualTo(1)
        expectThat(lastValue).isEqualTo(4)
    }

    @Test
    fun debounce_cancellationDuringDelay() = runTest {
        val scope = TaskScope(backgroundScope)
        var executions = 0

        scope.launch(testKey, TaskPolicy.Debounce(300.milliseconds)) {
            executions = 1
        }
        advanceTimeBy(100)
        scope.launch(testKey, TaskPolicy.Debounce(300.milliseconds)) {
            executions = 2
        }
        advanceTimeBy(300)
        runCurrent()
        expectThat(executions).isEqualTo(2)
    }

    @Test
    fun cancel_key_stopsJob() = runTest {
        val scope = TaskScope(backgroundScope)
        val completed = launchCancellableLongJob(scope)
        scope.cancel(testKey)
        advanceTimeBy(1_500)
        runCurrent()
        expectThat(completed.get()).isFalse()
        expectThat(scope.isActive(testKey)).isFalse()
    }

    @Test
    fun cancelAll_clearsRegistry() = runTest {
        val scope = TaskScope(backgroundScope)
        val completed = launchCancellableLongJob(scope)
        scope.cancelAll()
        advanceTimeBy(1_500)
        runCurrent()
        expectThat(completed.get()).isFalse()
        expectThat(scope.isActive(testKey)).isFalse()

        var relaunched = false
        val result = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            relaunched = true
        }
        runCurrent()
        expectThat(result).isA<TaskLaunchResult.Started>()
        expectThat(relaunched).isTrue()
    }

    /**
     * Starts a long SkipIfActive job that swallows cancellation; returns whether it completed.
     */
    private fun TestScope.launchCancellableLongJob(scope: TaskScope): AtomicBoolean {
        val completed = AtomicBoolean(false)
        scope.launch(testKey, TaskPolicy.SkipIfActive) {
            try {
                delay(1_000)
                completed.set(true)
            } catch (_: CancellationException) {
            }
        }
        runCurrent()
        return completed
    }

    /** Scope + Started handle after scheduling a long SkipIfActive job. */
    private fun TestScope.startLongSkipIfActiveJob(): Pair<TaskScope, TaskLaunchResult.Started> {
        val scope = TaskScope(backgroundScope)
        val started = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            delay(1_000)
        } as TaskLaunchResult.Started
        runCurrent()
        return scope to started
    }

    @Test
    fun blankKey_throws() {
        expectThrows<IllegalArgumentException> {
            TaskKey.of(" ")
        }
    }

    @Test
    fun parentScopeCancellation_cancelsChildTasks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val parentJob = SupervisorJob()
        val parentScope = CoroutineScope(parentJob + dispatcher)
        val scope = TaskScope(parentScope)
        var completed = false

        scope.launch(testKey, TaskPolicy.SkipIfActive) {
            try {
                delay(1_000)
                completed = true
            } catch (_: CancellationException) {
            }
        }
        runCurrent()
        parentJob.cancel()
        advanceTimeBy(1_500)
        runCurrent()
        expectThat(completed).isFalse()
    }

    @Test
    fun invokeOnCompletion_allowsRelaunchAfterComplete() = runTest {
        val scope = TaskScope(backgroundScope)
        var executions = 0

        scope.launch(testKey, TaskPolicy.SkipIfActive) {
            executions++
        }
        runCurrent()
        expectThat(scope.isActive(testKey)).isFalse()

        val second = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            executions++
        }
        runCurrent()
        expectThat(second).isA<TaskLaunchResult.Started>()
        expectThat(executions).isEqualTo(2)
    }

    @Test
    fun launch_usesDefaultSkipIfActivePolicy() = runTest {
        val scope = TaskScope(backgroundScope)
        var executions = 0

        scope.launch(testKey) {
            executions++
        }
        val skipped = scope.launch(testKey) {
            executions++
        }

        runCurrent()
        expectThat(skipped).isEqualTo(TaskLaunchResult.Skipped)
        expectThat(executions).isEqualTo(1)
    }

    @Test
    fun cancel_missingKey_isNoOp() = runTest {
        TaskScope(backgroundScope).cancel(TaskKey.of("missing"))
    }

    @Test
    fun isActive_missingKey_isFalse() = runTest {
        expectThat(TaskScope(backgroundScope).isActive(TaskKey.of("missing"))).isFalse()
    }

    @Test
    fun invokeOnCompletion_doesNotRemoveReplacementJob() = runTest {
        val scope = TaskScope(backgroundScope)

        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            try {
                delay(1_000)
            } catch (_: CancellationException) {
            }
        }
        scope.launch(testKey, TaskPolicy.ReplaceActive) {
            delay(100)
        }

        advanceTimeBy(150)
        runCurrent()
        expectThat(scope.isActive(testKey)).isFalse()
    }

    @Test
    fun taskState_startsIdleAndTracksRunning() = runTest {
        val scope = TaskScope(backgroundScope)
        val state = scope.taskState(testKey)

        expectThat(state.value).isEqualTo(TaskJobState.Idle)

        scope.launch(testKey, TaskPolicy.SkipIfActive) {
            delay(1_000)
        }
        runCurrent()
        expectThat(state.value).isEqualTo(TaskJobState.Running)

        scope.cancel(testKey)
        runCurrent()
        expectThat(state.value).isEqualTo(TaskJobState.Idle)
    }

    @Test
    fun cancelAll_resetsTaskState() = runTest {
        val scope = TaskScope(backgroundScope)
        val state = scope.taskState(testKey)

        scope.launch(testKey, TaskPolicy.SkipIfActive) {
            delay(1_000)
        }
        runCurrent()
        expectThat(state.value).isEqualTo(TaskJobState.Running)

        scope.cancelAll()
        runCurrent()
        expectThat(state.value).isEqualTo(TaskJobState.Idle)
    }

    @Test
    fun isActive_falseWhenJobCancelledViaHandleBeforeCompletionCallback() = runTest {
        val (scope, started) = startLongSkipIfActiveJob()
        expectThat(scope.isActive(testKey)).isTrue()
        started.handle.cancel()
        // Completion has not been drained, so the cancelled Job is still registered.
        expectThat(scope.isActive(testKey)).isFalse()

        val relaunched = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            delay(1_000)
        }
        expectThat(relaunched).isA<TaskLaunchResult.Started>()
        expectThat(scope.isActive(testKey)).isTrue()
    }

    @Test
    fun taskState_requiresDefaultTaskScope() {
        val unsupportedScope = object : TaskScope {
            override fun launch(key: TaskKey, policy: TaskPolicy, block: suspend CoroutineScope.() -> Unit): TaskLaunchResult = TaskLaunchResult.Skipped

            override fun cancel(key: TaskKey) = Unit

            override fun cancelAll() = Unit

            override fun isActive(key: TaskKey): Boolean = false
        }

        expectThrows<IllegalStateException> {
            unsupportedScope.taskState(testKey)
        }
    }

    @Test
    fun taskKey_equalityAndString() {
        val first = TaskKey.of("feature.action")
        val same = TaskKey.of("feature.action")
        val other = TaskKey.of("other")

        expectThat(first).isEqualTo(same)
        expectThat(first.hashCode()).isEqualTo(same.hashCode())
        expectThat(first.toString()).isEqualTo("feature.action")
        expectThat(first.equals(other)).isFalse()
        expectThat(first.equals("feature.action")).isFalse()
    }

    @Test
    fun taskHandle_exposesJobLifecycle() = runTest {
        val (_, started) = startLongSkipIfActiveJob()
        expectThat(started.handle.isActive).isTrue()
        started.handle.cancel()
        runCurrent()
        expectThat(started.handle.isActive).isFalse()
        started.handle.cancel(CancellationException("cancelled"))
    }

    @Test
    fun concurrentLaunchCancel_survivesMultiThreadStress() = runTest {
        val scope = TaskScope(backgroundScope)
        val state = scope.taskState(testKey)
        val threads = 8
        val iterations = 200
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val ops = AtomicInteger(0)

        repeat(threads) {
            pool.submit {
                start.await()
                repeat(iterations) { index ->
                    when (index % 3) {
                        0 -> scope.launch(testKey, TaskPolicy.ReplaceActive) {
                            delay(5)
                        }

                        1 -> scope.cancel(testKey)

                        else -> scope.launch(testKey, TaskPolicy.SkipIfActive) {
                            delay(5)
                        }
                    }
                    state.value
                    scope.isActive(testKey)
                    ops.incrementAndGet()
                }
            }
        }

        start.countDown()
        pool.shutdown()
        expectThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue()
        advanceTimeBy(1_000)
        runCurrent()
        expectThat(ops.get()).isEqualTo(threads * iterations)
        expectThat(state.value).isEqualTo(TaskJobState.Idle)
    }

    @Test
    fun taskHandle_equalityUsesJobIdentity() = runTest {
        val scope = TaskScope(backgroundScope)
        val first = scope.launch(testKey, TaskPolicy.SkipIfActive) {
            delay(1_000)
        } as TaskLaunchResult.Started
        val sameJob = first.handle
        val relaunched = scope.launch(testKey, TaskPolicy.ReplaceActive) {
            delay(1_000)
        } as TaskLaunchResult.Started

        runCurrent()
        expectThat(first.handle).isEqualTo(sameJob)
        expectThat(first.handle.hashCode()).isEqualTo(sameJob.hashCode())
        expectThat(first.handle).isNotEqualTo(relaunched.handle)
        expectThat(first.handle.equals("other")).isFalse()
    }
}
