package io.github.jdbenitez94.criollo.kmp.foundation.coroutines.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jdbenitez94.criollo.kmp.foundation.coroutines.TaskScope
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Property delegate that provides a [TaskScope] tied to [ViewModel.viewModelScope].
 *
 * ```
 * private val tasks by taskScope()
 * ```
 */
fun ViewModel.taskScope(): ReadOnlyProperty<Any?, TaskScope> = ViewModelTaskScopeProperty()

private class ViewModelTaskScopeProperty : ReadOnlyProperty<Any?, TaskScope> {
    private var cached: TaskScope? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): TaskScope {
        val viewModel = thisRef as ViewModel
        return cached ?: TaskScope(viewModel.viewModelScope).also { cached = it }
    }
}
