package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import kotlin.test.Test

/**
 * REQ-AND-01 — singleton identity for Context delegates (logic under [ContextDataStoreSingleton]).
 * Full Context file round-trips (REQ-AND-02/03) need androidHostTest / device; covered by
 * preferences/core jvmTest factories until host tests are wired in convention.
 */
class AndroidDelegateSingletonTest {
    @Test
    fun lazySingleton_returnsSameInstance() {
        var creates = 0
        val singleton = LazySingleton {
            creates++
            Any()
        }
        val first = singleton.get()
        val second = singleton.get()
        expectThat(first).isSameInstanceAs(second)
        expectThat(creates).isEqualTo(1)
    }
}
