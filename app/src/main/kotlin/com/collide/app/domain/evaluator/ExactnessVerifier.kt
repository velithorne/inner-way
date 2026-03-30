package com.collide.app.domain.evaluator

object ExactnessVerifier {
    fun verify(original: ByteArray, reconstructed: ByteArray): Boolean {
        if (original.size != reconstructed.size) return false
        return original.contentEquals(reconstructed)
    }
}
