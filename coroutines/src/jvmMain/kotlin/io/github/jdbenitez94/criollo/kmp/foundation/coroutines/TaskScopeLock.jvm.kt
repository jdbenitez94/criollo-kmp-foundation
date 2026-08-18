package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

internal actual class TaskScopeLock {
    private val lock = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
