package com.falcor.civilization.engine.util

import java.security.MessageDigest

object HashUtil {
    private val digest = MessageDigest.getInstance("SHA-256")

    fun sha256(input: String): String {
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256(vararg inputs: String): String {
        val combined = inputs.joinToString("")
        return sha256(combined)
    }
}
