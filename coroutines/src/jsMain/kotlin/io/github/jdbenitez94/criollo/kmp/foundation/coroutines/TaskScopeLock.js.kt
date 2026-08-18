package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

internal actual class TaskScopeLock {
    actual fun <T> withLock(block: () -> T): T = block()
}
