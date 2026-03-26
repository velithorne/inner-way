package com.aura.shell.knowledge

object TagNormalizer {
    fun normalize(raw: String): String {
        return raw
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
            .take(32)
            .trim()
    }

    fun displayFromKey(key: String): String = key
}
