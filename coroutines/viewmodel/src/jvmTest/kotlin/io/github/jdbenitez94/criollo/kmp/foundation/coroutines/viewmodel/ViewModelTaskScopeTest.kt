package io.github.jdbenitez94.criollo.kmp.foundation.coroutines.viewmodel

import androidx.lifecycle.ViewModel
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskScope
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs

private class TaskScopeViewModel : ViewModel() {
    val tasks by taskScope()
}

class ViewModelTaskScopeTest {

    @Nested
    inner class Delegate {

        @Test
        fun taskScope_delegateCachesScope() {
            val viewModel = TaskScopeViewModel()
            val first: TaskScope = viewModel.tasks
            val second: TaskScope = viewModel.tasks
            expectThat(first).isSameInstanceAs(second)
        }
    }
}
