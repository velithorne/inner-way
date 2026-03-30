package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType
import kotlin.math.abs

/**
 * Identifies candidates that appear structurally simpler than their parent
 * but maintain a similar token-type distribution — hinting at a more symmetric,
 * potentially equivalent structure.
 *
 * This is a heuristic structural hint — it does NOT prove behavioral equivalence.
 */
class SymmetryHintDetector : BaseDetector {
    override val detectorId = "symmetry_hint"
    override val detectorName = "Symmetry Hint"
    override val defaultThreshold = 0.25f

    override fun detect(
        candidateText: String,
        originalText: String,
        candidateB: String?,
        thresholdMultiplier: Float
    ): DetectorResult {
        if (candidateText.isBlank() || originalText.isBlank()) {
            return DetectorResult(
                detectorId, detectorName, 0f,
                defaultThreshold * thresholdMultiplier, false,
                "empty input", null
            )
        }

        val origProfile = tokenProfile(originalText)
        val candProfile = tokenProfile(candidateText)

        // Symmetry score: distribution similarity + compression
        val distribSimilarity = profileSimilarity(origProfile, candProfile)
        val sizeReduction = 1f - (candProfile.total.toFloat() / origProfile.total.coerceAtLeast(1))

        // A good symmetry hint: similar distribution but smaller
        val symmetryScore = when {
            sizeReduction <= 0 -> distribSimilarity * 0.3f  // expanded — less interesting
            else -> (distribSimilarity * 0.6f + sizeReduction.coerceIn(0f, 0.5f) * 0.8f)
                .coerceIn(0f, 1f)
        }

        val threshold = defaultThreshold * thresholdMultiplier
        val passed = symmetryScore >= threshold

        val reason = "distribution similarity=${pct(distribSimilarity)}%, " +
            "size change=${pct(sizeReduction)}%, symmetry=${pct(symmetryScore)}%"

        return DetectorResult(
            detectorId, detectorName, symmetryScore, threshold, passed, reason,
            if (passed) EventType.SYMMETRY_HINT else null
        )
    }

    private data class TokenProfile(
        val keywords: Int,
        val operators: Int,
        val literals: Int,
        val identifiers: Int,
        val total: Int
    )

    private fun tokenProfile(text: String): TokenProfile {
        val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        var kw = 0; var op = 0; var lit = 0; var id = 0
        val keywords = setOf("if", "else", "for", "while", "return", "fun", "def", "function",
            "class", "val", "var", "let", "const", "import", "true", "false", "null")
        val opChars = setOf("+", "-", "*", "/", "=", "<", ">", "!", "&", "|", "%", "==",
            "!=", "<=", ">=", "&&", "||", "+=", "-=")
        for (tok in tokens) {
            when {
                tok in keywords -> kw++
                tok in opChars -> op++
                tok.firstOrNull()?.isDigit() == true || tok.startsWith("\"") || tok.startsWith("'") -> lit++
                tok.firstOrNull()?.isLetter() == true -> id++
            }
        }
        return TokenProfile(kw, op, lit, id, tokens.size)
    }

    private fun profileSimilarity(a: TokenProfile, b: TokenProfile): Float {
        if (a.total == 0 || b.total == 0) return 0f
        fun ratio(v: Int, total: Int) = v.toFloat() / total
        val diffs = listOf(
            abs(ratio(a.keywords, a.total) - ratio(b.keywords, b.total)),
            abs(ratio(a.operators, a.total) - ratio(b.operators, b.total)),
            abs(ratio(a.literals, a.total) - ratio(b.literals, b.total)),
            abs(ratio(a.identifiers, a.total) - ratio(b.identifiers, b.total))
        )
        return (1f - diffs.average().toFloat()).coerceIn(0f, 1f)
    }

    private fun pct(f: Float): String = "%.0f".format(f * 100)
}
