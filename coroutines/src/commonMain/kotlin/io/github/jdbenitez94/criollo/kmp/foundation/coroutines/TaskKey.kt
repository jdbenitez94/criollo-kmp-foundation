package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

/**
 * Stable identifier for a managed coroutine task within a [TaskScope].
 *
 * Prefer defining domain keys in the app layer (for example a sealed hierarchy with `TaskKey.of(...)`)
 * instead of scattering raw strings at call sites.
 */
class TaskKey private constructor(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskKey must not be blank" }
    }

    override fun equals(other: Any?): Boolean = other is TaskKey && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        fun of(value: String): TaskKey = TaskKey(value)
    }
}
