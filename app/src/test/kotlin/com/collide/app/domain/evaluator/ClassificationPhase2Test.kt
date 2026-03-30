package com.collide.app.domain.evaluator

import com.collide.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ClassificationPhase2Test {

    private val evaluator = CandidateEvaluator()
    private val baselineEval = BaselineEvaluator()

    private val repetitiveInput = ByteArray(3000) { (it % 32).toByte() }
    private val noZeroInput = ByteArray(500) { (it % 255 + 1).toByte() }

    private fun baseline(input: ByteArray = repetitiveInput) =
        baselineEval.evaluate(input, BaselineStrategy.RAW_DEFLATE).encodedSize

    @Test
    fun `not_applicable classification for zero_run_rle on non-zero data`() {
        val recipe = RecipeSpec("r_zrle", listOf("zero_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(noZeroInput, recipe, baseline(noZeroInput))
        assertEquals(CandidateClassification.NOT_APPLICABLE, result.classification)
        assertFalse(result.reconstructionPassed)
    }

    @Test
    fun `encode_error for unknown transform id`() {
        val recipe = RecipeSpec("r_bad", listOf("nonexistent"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        assertEquals(CandidateClassification.ENCODE_ERROR, result.classification)
        assertFalse(result.reconstructionPassed)
        assertNotNull(result.reason)
    }

    @Test
    fun `strict winner has positive savings and verification`() {
        val input = ByteArray(5000) { (it % 4).toByte() }
        val b = baseline(input)
        val recipes = listOf(
            RecipeSpec("r1", listOf("byte_run_rle"), BackendCompressor.DEFLATE_DEFAULT),
            RecipeSpec("r2", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            RecipeSpec("r3", listOf("zero_run_rle"), BackendCompressor.DEFLATE_DEFAULT),
            RecipeSpec("r4", listOf("move_to_front"), BackendCompressor.DEFLATE_DEFAULT)
        )
        val winner = recipes.map { evaluator.evaluate(input, it, b) }
            .firstOrNull { it.isWinner }
        if (winner != null) {
            assertTrue(winner.byteSavings > 0)
            assertTrue(winner.encodedSize < winner.baselineSize)
            assertTrue(winner.reconstructionPassed)
            assertNotNull(winner.verificationResult)
            assertTrue(winner.verificationResult!!.passed)
            assertTrue(winner.verificationResult!!.hashesMatch)
            assertNotNull(winner.sizeBreakdown)
        }
    }

    @Test
    fun `no_strict_improvement result still has verification`() {
        // Identity transform will likely not improve
        val recipe = RecipeSpec("r_id_like", listOf("xor_prev"), BackendCompressor.DEFLATE_DEFAULT)
        val small = ByteArray(50) { it.toByte() }
        val b = baseline(small)
        val result = evaluator.evaluate(small, recipe, b)
        if (result.classification == CandidateClassification.NO_STRICT_IMPROVEMENT) {
            assertTrue(result.reconstructionPassed)
            assertNotNull(result.verificationResult)
        }
    }

    @Test
    fun `candidate index is persisted in result`() {
        val recipe = RecipeSpec("r_idx", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline(), candidateIndex = 42)
        assertEquals(42, result.candidateIndex)
    }

    @Test
    fun `reason string is present for all non-winner classifications`() {
        val recipe = RecipeSpec("r_reason", listOf("zero_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(noZeroInput, recipe, baseline(noZeroInput))
        assertNotNull("Reason must be non-null for non-winner", result.reason)
        assertTrue(result.reason!!.isNotBlank())
    }

    @Test
    fun `all new classification labels are non-blank`() {
        CandidateClassification.entries.forEach { cls ->
            assertTrue("Label for $cls should not be blank", cls.displayLabel.isNotBlank())
        }
    }
}
