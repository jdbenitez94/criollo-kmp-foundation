package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

/** Minimal JSON string-array parser for worker `listKeyIds` payloads (`["a","b"]`). */
internal fun parseJsonStringArray(json: String): List<String> {
    val trimmed = json.trim()
    require(trimmed.startsWith("[") && trimmed.endsWith("]")) { "Expected JSON string array." }
    val body = trimmed.substring(1, trimmed.lastIndex).trim()
    if (body.isEmpty()) return emptyList()
    return body.split(',')
        .map { token ->
            val value = token.trim()
            require(value.length >= 2 && value.first() == '"' && value.last() == '"') {
                "Expected JSON string element."
            }
            value.substring(1, value.lastIndex)
        }
}
