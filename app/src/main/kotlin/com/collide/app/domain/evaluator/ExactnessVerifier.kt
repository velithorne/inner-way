package com.collide.app.domain.evaluator

import com.collide.app.domain.model.VerificationMethod
import com.collide.app.domain.model.VerificationResult
import java.security.MessageDigest

object ExactnessVerifier {

    /** Legacy byte-only check used by callers that don't need the full VerificationResult. */
    fun verify(original: ByteArray, reconstructed: ByteArray): Boolean {
        if (original.size != reconstructed.size) return false
        return original.contentEquals(reconstructed)
    }

    /**
     * Phase 2 verification: byte-for-byte equality AND SHA-256 hash match.
     * Both conditions must hold for passed == true.
     */
    fun verifyFull(original: ByteArray, reconstructed: ByteArray): VerificationResult {
        val originalHash = sha256Hex(original)
        val reconstructedHash = sha256Hex(reconstructed)

        val lengthOk = original.size == reconstructed.size
        val bytesMatch = lengthOk && original.contentEquals(reconstructed)
        val hashesMatch = originalHash == reconstructedHash

        val passed = bytesMatch && hashesMatch
        val failureReason = when {
            !lengthOk -> "Length mismatch: original=${original.size} reconstructed=${reconstructed.size}"
            !bytesMatch -> "Byte content differs despite equal length"
            !hashesMatch -> "SHA-256 hash mismatch (should not happen if bytes match — indicates internal error)"
            else -> null
        }

        return VerificationResult(
            passed = passed,
            method = VerificationMethod.BYTE_EQUALITY_AND_SHA256,
            originalSha256 = originalHash,
            reconstructedSha256 = reconstructedHash,
            failureReason = failureReason
        )
    }

    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
