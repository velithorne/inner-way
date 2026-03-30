package com.collide.app.domain.model

data class SavedEvent(
    val id: Long,
    val timestamp: Long,
    val inputFileName: String,
    val inputSize: Long,
    val baselineType: String,
    val baselineSize: Long,
    val winningSize: Long,
    val byteSavings: Long,
    val recipeSummary: String,
    val recipeJson: String,
    val verificationPassed: Boolean,
    val elapsedMs: Long,
    val notes: String = "",
    // Phase 2 fields
    val originalSha256: String = "",
    val reconstructedSha256: String = "",
    val verificationMethod: String = "BYTE_EQUALITY_ONLY",
    val transformedPayloadSize: Long = 0L,
    val backendCompressedSize: Long = 0L,
    val transformMetadataSize: Long = 0L,
    val containerHeaderSize: Long = 8L,
    val candidateIndex: Int = -1,
    val runConfigJson: String = "",
    val engineVersion: String = "1.0.0-phase1",
    val replayStatus: String = ReplayStatus.PENDING.name
) {
    val compressionRatioPct: Double
        get() = if (baselineSize > 0) (byteSavings.toDouble() / baselineSize) * 100.0 else 0.0

    val hashesMatch: Boolean
        get() = originalSha256.isNotEmpty() && originalSha256 == reconstructedSha256

    fun sizeBreakdown(): SizeBreakdown? {
        if (backendCompressedSize == 0L) return null
        return SizeBreakdown(
            transformedPayloadSize = transformedPayloadSize,
            backendCompressedPayloadSize = backendCompressedSize,
            transformMetadataSize = transformMetadataSize,
            containerHeaderSize = containerHeaderSize,
            originalInputSize = inputSize
        )
    }
}
