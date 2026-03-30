package com.collide.app.domain.model

enum class ReplayStatus {
    MATCHED,        // Re-run produced identical total encoded size and verification passed
    SIZE_MISMATCH,  // Re-run completed but size differs from archived event
    VERIFY_FAILED,  // Re-run failed exactness check
    ERROR,          // Re-run threw an exception
    PENDING         // Not yet replayed
}

data class ReplayResult(
    val status: ReplayStatus,
    val replayEncodedSize: Long,
    val replayVerificationPassed: Boolean,
    val replayElapsedMs: Long,
    val replayOriginalSha256: String = "",
    val replayReconstructedSha256: String = "",
    val errorMessage: String? = null
)
