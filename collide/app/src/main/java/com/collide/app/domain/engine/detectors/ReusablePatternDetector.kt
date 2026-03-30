package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType

/**
 * Detects whether the candidate contains repeated structural patterns (scaffolds),
 * which may indicate extractable reusable structure.
 *
 * High score = multiple occurrences of the same sub-pattern.
 * This measures structural repetition, not semantic utility.
 */
class ReusablePatternDetector : BaseDetector {
    override val detectorId = "reusable_pattern"
    override val detectorName = "Reusable Pattern"
    override val defaultThreshold = 0.2f

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

        val tokens = candidateText.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 6) {
            return DetectorResult(
                detectorId, detectorName, 0f,
                defaultThreshold * thresholdMultiplier, false,
                "too few tokens to detect patterns (${tokens.size})", null
            )
        }

        val ngramCounts = mutableMapOf<String, Int>()
        val n = 3
        for (i in 0..tokens.size - n) {
            val gram = tokens.subList(i, i + n).joinToString(" ")
            ngramCounts[gram] = (ngramCounts[gram] ?: 0) + 1
        }

        val repeatedGrams = ngramCounts.filter { it.value >= 2 }
        val totalRepeated = repeatedGrams.values.sum()
        val uniqueRepeated = repeatedGrams.size

        val patternScore = (uniqueRepeated.toFloat() / (tokens.size / n).coerceAtLeast(1))
            .coerceIn(0f, 1f)
        val threshold = defaultThreshold * thresholdMultiplier
        val passed = patternScore >= threshold

        val reason = when {
            uniqueRepeated == 0 -> "no repeated n-gram patterns found"
            uniqueRepeated < 2 -> "1 repeated pattern: \"${repeatedGrams.keys.first().take(20)}\""
            else -> "$uniqueRepeated unique repeated patterns (total $totalRepeated occurrences)"
        }

        return DetectorResult(
            detectorId, detectorName, patternScore, threshold, passed, reason,
            if (passed) EventType.REUSABLE_SCAFFOLD_EXTRACTED else null
        )
    }
}
