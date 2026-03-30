package com.collide.app.domain.engine.detectors

import com.collide.app.domain.model.DetectorResult
import com.collide.app.domain.model.EventType
import kotlin.math.abs

/**
 * Flags when two seemingly similar inputs produce strongly different normalized structures,
 * or when a candidate shows internal structural tension (conflicting patterns).
 *
 * This is a heuristic tension detector — it does NOT prove logical contradiction.
 */
class ContradictionDetector : BaseDetector {
    override val detectorId = "contradiction"
    override val detectorName = "Contradiction/Tension"
    override val defaultThreshold = 0.3f

    override fun detect(
        candidateText: String,
        originalText: String,
        candidateB: String?,
        thresholdMultiplier: Float
    ): DetectorResult {
        val threshold = defaultThreshold * thresholdMultiplier

        if (candidateText.isBlank()) {
            return DetectorResult(
                detectorId, detectorName, 0f, threshold, false,
                "empty candidate", null
            )
        }

        val score = if (candidateB != null) {
            detectDualInputContradiction(candidateText, originalText, candidateB)
        } else {
            detectInternalContradiction(candidateText, originalText)
        }

        val passed = score >= threshold
        val reason = buildReason(score, candidateText, originalText, candidateB)

        return DetectorResult(
            detectorId, detectorName, score, threshold, passed, reason,
            if (passed) EventType.CONTRADICTION_DETECTED else null
        )
    }

    private fun detectDualInputContradiction(candidate: String, a: String, b: String): Float {
        // Check: if A and B look similar but candidate looks different from both
        val simAB = jaccardSimilarity(roughTokens(a), roughTokens(b))
        val simCandA = jaccardSimilarity(roughTokens(candidate), roughTokens(a))
        val simCandB = jaccardSimilarity(roughTokens(candidate), roughTokens(b))

        return when {
            simAB > 0.5f && simCandA < 0.3f && simCandB < 0.3f ->
                // Similar parents, very different candidate — strong tension
                (simAB - simCandA + simAB - simCandB) / 2f
            simAB > 0.3f && abs(simCandA - simCandB) > 0.3f ->
                // Asymmetric inheritance — moderate tension
                abs(simCandA - simCandB) * 0.7f
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    private fun detectInternalContradiction(candidate: String, original: String): Float {
        val origTokens = roughTokens(original)
        val candTokens = roughTokens(candidate)

        if (origTokens.isEmpty() || candTokens.isEmpty()) return 0f

        val origKeywords = origTokens.filter { isKeyword(it) }.toSet()
        val candKeywords = candTokens.filter { isKeyword(it) }.toSet()

        val lostKeywords = origKeywords - candKeywords
        val gainedKeywords = candKeywords - origKeywords

        // Contradiction: lost important structural keywords, gained different ones
        val importantLost = lostKeywords.count { it in STRUCTURAL_KEYWORDS }
        val importantGained = gainedKeywords.count { it in STRUCTURAL_KEYWORDS }

        return when {
            importantLost > 0 && importantGained > 0 ->
                ((importantLost + importantGained).toFloat() / STRUCTURAL_KEYWORDS.size)
                    .coerceIn(0f, 1f) * 0.8f
            importantLost > 2 -> (importantLost.toFloat() / 5f).coerceIn(0f, 0.5f)
            else -> 0f
        }
    }

    private fun buildReason(score: Float, candidate: String, a: String, b: String?): String {
        return when {
            score < 0.1f -> "no significant structural tension detected"
            score < 0.3f -> "minor structural tension (${pct(score)})"
            score < 0.6f -> "moderate contradiction: structural patterns conflict (${pct(score)})"
            else -> "strong structural contradiction detected (${pct(score)})"
        }
    }

    private fun jaccardSimilarity(a: List<String>, b: List<String>): Float {
        val sa = a.toSet(); val sb = b.toSet()
        val inter = sa.intersect(sb).size
        val union = sa.union(sb).size
        return if (union == 0) 1f else inter.toFloat() / union
    }

    private fun roughTokens(text: String): List<String> =
        text.split(Regex("\\s+")).filter { it.isNotBlank() }

    private fun isKeyword(tok: String): Boolean = tok.lowercase() in ALL_KEYWORDS

    private val ALL_KEYWORDS = setOf(
        "if", "else", "for", "while", "return", "fun", "def", "function",
        "class", "val", "var", "let", "const", "import", "true", "false",
        "null", "break", "continue", "try", "catch", "throw"
    )

    private val STRUCTURAL_KEYWORDS = setOf(
        "if", "for", "while", "return", "class", "fun", "def", "function"
    )

    private fun pct(f: Float): String = "%.0f".format(f * 100)
}
