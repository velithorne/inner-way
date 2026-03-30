package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType

/**
 * Detects structural compression: whether the candidate has fewer tokens/lines
 * while remaining structurally coherent (non-empty, plausible structure).
 *
 * A high score indicates meaningfully reduced complexity.
 * This does NOT claim semantic equivalence — it measures size reduction only.
 */
class StructuralCompressionDetector : BaseDetector {
    override val detectorId = "structural_compression"
    override val detectorName = "Structural Compression"
    override val defaultThreshold = 0.1f  // at least 10% compression

    override fun detect(
        candidateText: String,
        originalText: String,
        candidateB: String?,
        thresholdMultiplier: Float
    ): DetectorResult {
        if (candidateText.isBlank()) {
            return DetectorResult(
                detectorId, detectorName, 0f,
                defaultThreshold * thresholdMultiplier, false,
                "empty candidate", null
            )
        }

        val origTokens = roughTokenCount(originalText)
        val candTokens = roughTokenCount(candidateText)

        if (origTokens == 0) {
            return DetectorResult(
                detectorId, detectorName, 0f,
                defaultThreshold * thresholdMultiplier, false,
                "original has no tokens", null
            )
        }

        val compressionRatio = 1f - (candTokens.toFloat() / origTokens.toFloat())

        // Validate candidate is still coherent (not trivially empty)
        val coherent = candTokens > 2 && candTokens < origTokens * 2
        val threshold = defaultThreshold * thresholdMultiplier

        return if (!coherent) {
            DetectorResult(
                detectorId, detectorName, 0f, threshold, false,
                "candidate not coherent: token count $candTokens vs original $origTokens", null
            )
        } else if (compressionRatio > 0) {
            val passed = compressionRatio >= threshold
            DetectorResult(
                detectorId, detectorName,
                compressionRatio.coerceIn(0f, 1f), threshold, passed,
                "${pct(compressionRatio)}% compression ($origTokens → $candTokens tokens)",
                if (passed) EventType.STRUCTURAL_SIMPLIFICATION else null
            )
        } else {
            val expansion = -compressionRatio
            DetectorResult(
                detectorId, detectorName, 0f, threshold, false,
                "candidate is larger by ${pct(expansion)}% ($origTokens → $candTokens tokens)", null
            )
        }
    }

    private fun roughTokenCount(text: String): Int =
        text.split(Regex("\\s+")).count { it.isNotBlank() }

    private fun pct(f: Float): String = "%.0f".format(f * 100)
}
