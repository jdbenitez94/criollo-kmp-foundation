package io.github.jdbenitez94.criollo.kmp.foundation.coroutines

import platform.Foundation.NSRecursiveLock

internal actual class TaskScopeLock {
    // Must be recursive: job.start()/cancel() under the lock can run invokeOnCompletion
    // synchronously on Unconfined/Main.immediate, which re-enters withLock.
    private val lock = NSRecursiveLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
