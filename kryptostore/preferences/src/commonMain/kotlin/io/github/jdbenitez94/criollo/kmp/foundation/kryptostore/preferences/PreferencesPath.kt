package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences

import okio.Path

/** AndroidX Preferences DataStore requires the `.preferences_pb` file extension (REQ-STO-07). */
const val PREFERENCES_PB_EXTENSION = "preferences_pb"

fun Path.requirePreferencesPbExtension(): Path {
    val name = this.name
    require(name.endsWith(".$PREFERENCES_PB_EXTENSION")) {
        "Preferences DataStore path must end with '.$PREFERENCES_PB_EXTENSION' (got '$name')."
    }
    return this
}
