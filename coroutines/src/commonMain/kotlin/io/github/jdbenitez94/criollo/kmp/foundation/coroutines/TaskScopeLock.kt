package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

internal expect class TaskScopeLock() {
    fun <T> withLock(block: () -> T): T
}
