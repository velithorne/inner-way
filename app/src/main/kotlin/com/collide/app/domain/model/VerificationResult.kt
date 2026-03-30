package com.collide.app.domain.model

enum class VerificationMethod {
    BYTE_EQUALITY_AND_SHA256,
    BYTE_EQUALITY_ONLY  // fallback, should not occur in Phase 2
}

/**
 * Result of the mandatory exactness check.
 *
 * Phase 2 requires BOTH byte-for-byte equality AND matching SHA-256 hashes.
 * A candidate is only admissible if passed == true.
 */
data class VerificationResult(
    val passed: Boolean,
    val method: VerificationMethod,
    val originalSha256: String,
    val reconstructedSha256: String,
    val failureReason: String? = null
) {
    val hashesMatch: Boolean get() = originalSha256 == reconstructedSha256
}
