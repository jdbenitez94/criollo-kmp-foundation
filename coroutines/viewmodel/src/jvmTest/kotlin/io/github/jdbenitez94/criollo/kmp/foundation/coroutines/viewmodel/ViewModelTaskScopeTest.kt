package io.github.jdbenitez94.criollo.kmp.foundation.coroutines.viewmodel

import androidx.lifecycle.ViewModel
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskScope
import strikt.api.expectThat
import strikt.assertions.isSameInstanceAs
import kotlin.test.Test

private class TaskScopeViewModel : ViewModel() {
    val tasks by taskScope()
}

class ViewModelTaskScopeTest {

    @Test
    fun taskScope_delegateCachesScope() {
        val viewModel = TaskScopeViewModel()
        val first: TaskScope = viewModel.tasks
        val second: TaskScope = viewModel.tasks
        expectThat(first).isSameInstanceAs(second)
    }
}
