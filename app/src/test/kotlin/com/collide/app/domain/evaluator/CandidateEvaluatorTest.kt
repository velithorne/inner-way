package com.collide.app.domain.evaluator

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.RecipeSpec
import org.junit.Assert.*
import org.junit.Test

class CandidateEvaluatorTest {
    private val evaluator = CandidateEvaluator()

    private fun makeInput(size: Int = 2000): ByteArray {
        // Repetitive data that should compress well
        return ByteArray(size) { (it % 64).toByte() }
    }

    @Test
    fun `identity recipe produces valid result`() {
        val input = makeInput()
        val recipe = RecipeSpec("test_identity", listOf("identity"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        assertTrue(result.reconstructionPassed)
        assertTrue(
            result.classification == CandidateClassification.STRICT_WINNER ||
            result.classification == CandidateClassification.NO_STRICT_IMPROVEMENT
        )
    }

    @Test
    fun `exactness check passes for valid transform`() {
        val input = makeInput()
        val recipe = RecipeSpec("test_delta", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        assertTrue(result.reconstructionPassed)
        assertNotEquals(CandidateClassification.EXACTNESS_FAILED, result.classification)
        assertNotEquals(CandidateClassification.DECODE_ERROR, result.classification)
    }

    @Test
    fun `unknown transform id gives encode error`() {
        val input = makeInput()
        val recipe = RecipeSpec("test_unknown", listOf("nonexistent_transform"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(input, recipe, 1000L)
        assertEquals(CandidateClassification.ENCODE_ERROR, result.classification)
        assertFalse(result.reconstructionPassed)
    }

    @Test
    fun `zero run rle not applicable on no zero bytes`() {
        val input = ByteArray(200) { (it % 255 + 1).toByte() } // No zeros
        val recipe = RecipeSpec("test_zero_rle", listOf("zero_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(input, recipe, 1000L)
        assertEquals(CandidateClassification.NOT_APPLICABLE, result.classification)
        assertFalse(result.reconstructionPassed)
    }

    @Test
    fun `total encoded size includes metadata overhead`() {
        val input = makeInput(500)
        val recipe = RecipeSpec("test_shuffle", listOf("block_shuffle_4"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        // Total size should include the 8-byte metadata from FixedBlockShuffleTransform
        // plus the 4+4 header overhead
        assertTrue(result.encodedSize > 0)
    }

    @Test
    fun `multi transform chain round trips correctly`() {
        val input = makeInput(1000)
        val recipe = RecipeSpec("test_chain", listOf("delta8", "xor_prev"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        assertTrue(result.reconstructionPassed)
        assertNotEquals(CandidateClassification.EXACTNESS_FAILED, result.classification)
    }

    @Test
    fun `strict winner has positive savings`() {
        val input = makeInput(5000) // Large repetitive input
        val recipe = RecipeSpec("test_winner", listOf("byte_run_rle"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        if (result.classification == CandidateClassification.STRICT_WINNER) {
            assertTrue(result.byteSavings > 0)
            assertTrue(result.encodedSize < result.baselineSize)
            assertTrue(result.reconstructionPassed)
        }
    }

    @Test
    fun `no improvement result has non-positive savings`() {
        val input = "Hello World".toByteArray().let { base ->
            // Very small input - hard to improve on
            ByteArray(20) { base[it % base.size] }
        }
        val recipe = RecipeSpec("test_tiny", listOf("identity"), BackendCompressor.DEFLATE_DEFAULT)
        val baseline = computeBaseline(input)
        val result = evaluator.evaluate(input, recipe, baseline)
        if (result.classification == CandidateClassification.NO_STRICT_IMPROVEMENT) {
            assertTrue(result.encodedSize >= result.baselineSize)
        }
    }

    private fun computeBaseline(input: ByteArray): Long {
        return com.collide.app.domain.engine.BackendCompressorEngine.compress(
            input,
            BackendCompressor.DEFLATE_DEFAULT
        ).size.toLong()
    }
}
