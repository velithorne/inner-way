package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType

/**
 * Identifies non-trivial merged structures from two inputs.
 * A meaningful hybridization shows structural features from both parents
 * rather than just concatenation.
 *
 * This only applies to dual-input candidates.
 */
class HybridizationDetector : BaseDetector {
    override val detectorId = "hybridization"
    override val detectorName = "Hybridization"
    override val defaultThreshold = 0.2f

    override fun detect(
        candidateText: String,
        originalText: String,
        candidateB: String?,
        thresholdMultiplier: Float
    ): DetectorResult {
        val threshold = defaultThreshold * thresholdMultiplier

        if (candidateB == null) {
            return DetectorResult(
                detectorId, detectorName, 0f, threshold, false,
                "hybridization requires two inputs", null
            )
        }

        if (candidateText.isBlank()) {
            return DetectorResult(
                detectorId, detectorName, 0f, threshold, false,
                "empty candidate", null
            )
        }

        val tokensA = roughTokenSet(originalText)
        val tokensB = roughTokenSet(candidateB)
        val tokensCand = roughTokenSet(candidateText)

        val fromA = tokensCand.intersect(tokensA).size.toFloat()
        val fromB = tokensCand.intersect(tokensB).size.toFloat()
        val total = tokensCand.size.toFloat().coerceAtLeast(1f)

        val fractionFromA = fromA / total
        val fractionFromB = fromB / total

        // A good hybrid draws from both parents meaningfully
        val hybridScore = when {
            fractionFromA < 0.05f || fractionFromB < 0.05f -> 0f  // barely from one parent
            else -> (fractionFromA.coerceAtMost(0.6f) + fractionFromB.coerceAtMost(0.6f)) / 1.2f
        }.coerceIn(0f, 1f)

        val passed = hybridScore >= threshold

        val reason = "candidate draws ${pct(fractionFromA)}% from A and ${pct(fractionFromB)}% from B"

        return DetectorResult(
            detectorId, detectorName, hybridScore, threshold, passed, reason,
            when {
                passed && hybridScore > 0.5f -> EventType.HYBRID_STRUCTURE_FOUND
                passed -> EventType.UNEXPECTED_MERGE
                else -> null
            }
        )
    }

    private fun roughTokenSet(text: String): Set<String> =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

    private fun pct(f: Float): String = "%.0f".format(f * 100)
}
