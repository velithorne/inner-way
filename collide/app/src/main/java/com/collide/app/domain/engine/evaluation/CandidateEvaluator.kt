package com.collide.app.domain.engine.evaluation

import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.CandidateResult
import com.collide.app.domain.model.NormalizedCodeModel

/**
 * Lightweight structural evaluation of candidates.
 *
 * Phase 1 evaluations:
 * 1. Structural validity under the app's own representation rules
 * 2. Candidate completeness (non-empty, plausible structure)
 * 3. Structural consistency checks
 *
 * This evaluator does NOT claim full semantic correctness for arbitrary code.
 * If no real behavioral test is available, it says so clearly.
 */
class CandidateEvaluator {

    fun validateStructure(candidate: CandidateResult, originalModel: NormalizedCodeModel): ValidationResult {
        val text = candidate.candidateText

        if (text.isBlank()) {
            return ValidationResult.Invalid("candidate text is empty")
        }

        if (text.length < 2) {
            return ValidationResult.Invalid("candidate too short to be meaningful")
        }

        // Bracket balance check — a basic structural consistency check
        val bracketResult = checkBracketBalance(text)
        if (bracketResult != null) {
            // Imbalanced brackets are flagged but not necessarily fatal
            return ValidationResult.Partial(
                "bracket imbalance: $bracketResult — structural validity uncertain"
            )
        }

        // Check candidate is not identical to original (would be uninteresting)
        if (text.trim() == originalModel.tokens.joinToString(" ") { it.text }.trim()) {
            return ValidationResult.Uninteresting("candidate is identical to original")
        }

        return ValidationResult.Valid(
            "structural checks passed — note: no behavioral test performed"
        )
    }

    private fun checkBracketBalance(text: String): String? {
        val stack = ArrayDeque<Char>()
        val pairs = mapOf(')' to '(', '}' to '{', ']' to '[')

        for (ch in text) {
            when {
                ch in listOf('(', '{', '[') -> stack.addLast(ch)
                ch in pairs -> {
                    val expected = pairs[ch]
                    if (stack.isEmpty() || stack.last() != expected) {
                        return "unexpected '$ch' without matching '${expected}'"
                    }
                    stack.removeLast()
                }
            }
        }
        return if (stack.isNotEmpty()) "unclosed brackets: ${stack.takeLast(3).joinToString("")}" else null
    }
}

sealed class ValidationResult {
    data class Valid(val note: String) : ValidationResult()
    data class Partial(val reason: String) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
    data class Uninteresting(val reason: String) : ValidationResult()
}
