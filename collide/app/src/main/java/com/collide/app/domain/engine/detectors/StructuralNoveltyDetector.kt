package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType
import kotlin.math.abs

/**
 * Measures how structurally different the candidate is from its parent input.
 * Uses normalized edit-distance approximation on token-level representation.
 * Higher score = more novel.
 */
class StructuralNoveltyDetector : BaseDetector {
    override val detectorId = "structural_novelty"
    override val detectorName = "Structural Novelty"
    override val defaultThreshold = 0.15f  // 15% structural change minimum

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

        val origTokens = roughTokens(originalText)
        val candTokens = roughTokens(candidateText)

        val noveltyScore = computeNovelty(origTokens, candTokens)
        val threshold = defaultThreshold * thresholdMultiplier
        val passed = noveltyScore >= threshold

        val reason = when {
            noveltyScore < 0.05f -> "almost identical to parent (${pct(noveltyScore)}% change)"
            noveltyScore < threshold -> "minor changes (${pct(noveltyScore)}% change, need ${pct(threshold)}%)"
            noveltyScore < 0.5f -> "moderate novelty — ${pct(noveltyScore)}% structural change"
            else -> "high novelty — ${pct(noveltyScore)}% structural change"
        }

        return DetectorResult(
            detectorId, detectorName, noveltyScore, threshold, passed, reason,
            if (passed) EventType.COHERENT_VARIANT else null
        )
    }

    private fun computeNovelty(orig: List<String>, cand: List<String>): Float {
        if (orig.isEmpty() && cand.isEmpty()) return 0f
        if (orig.isEmpty()) return 1f
        if (cand.isEmpty()) return 1f

        val origSet = orig.toSet()
        val candSet = cand.toSet()
        val intersection = origSet.intersect(candSet).size
        val union = origSet.union(candSet).size

        val jaccardSimilarity = if (union == 0) 1f else intersection.toFloat() / union
        val novelty = 1f - jaccardSimilarity

        // Also factor in size change
        val sizeRatio = abs(orig.size - cand.size).toFloat() / orig.size.coerceAtLeast(1)

        return (novelty * 0.7f + sizeRatio.coerceAtMost(1f) * 0.3f).coerceIn(0f, 1f)
    }

    private fun roughTokens(text: String): List<String> =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun pct(f: Float): String = "%.0f".format(f * 100)
}
