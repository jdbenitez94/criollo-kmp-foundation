package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import kotlinx.coroutines.Job
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty

class RemoveJobIfCurrentTest {

    @Test
    fun removeJobIfCurrent_removesMatchingJob() {
        val jobs = mutableMapOf<TaskKey, Job>()
        val key = TaskKey.of("cleanup")
        val job = Job()

        jobs[key] = job
        removeJobIfCurrent(jobs, key, job)

        expectThat(jobs).isEmpty()
    }

    @Test
    fun removeJobIfCurrent_skipsStaleJob() {
        val jobs = mutableMapOf<TaskKey, Job>()
        val key = TaskKey.of("cleanup")
        jobs[key] = Job()

        removeJobIfCurrent(jobs, key, Job())

        expectThat(jobs).hasSize(1)
    }
}
