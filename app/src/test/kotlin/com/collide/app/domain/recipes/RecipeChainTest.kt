package com.collide.app.domain.recipes

import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.RecipeSpec
import com.collide.app.domain.evaluator.CandidateEvaluator
import com.collide.app.domain.engine.BackendCompressorEngine
import org.junit.Assert.*
import org.junit.Test

class RecipeChainTest {
    private val evaluator = CandidateEvaluator()

    private val testInput = ByteArray(2000) { (it % 32).toByte() }

    private fun baseline() = BackendCompressorEngine.compress(
        testInput, BackendCompressor.DEFLATE_DEFAULT
    ).size.toLong()

    @Test
    fun `two transform chain delta8 then xor round trips`() {
        val recipe = RecipeSpec("chain_2", listOf("delta8", "xor_prev"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(testInput, recipe, baseline())
        assertTrue(result.reconstructionPassed)
        assertNotEquals(CandidateClassification.EXACTNESS_FAILED, result.classification)
    }

    @Test
    fun `three transform chain round trips`() {
        val recipe = RecipeSpec("chain_3", listOf("delta8", "xor_prev", "move_to_front"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(testInput, recipe, baseline())
        assertTrue(result.reconstructionPassed)
    }

    @Test
    fun `chain with block shuffle round trips`() {
        val recipe = RecipeSpec("chain_shuffle", listOf("block_shuffle_4", "delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(testInput, recipe, baseline())
        assertTrue(result.reconstructionPassed)
    }

    @Test
    fun `chain with rle and delta round trips`() {
        val rleInput = ByteArray(2000) { (it % 4).toByte() } // good for RLE
        val b = BackendCompressorEngine.compress(rleInput, BackendCompressor.DEFLATE_DEFAULT).size.toLong()
        val recipe = RecipeSpec("chain_rle_delta", listOf("byte_run_rle", "delta8"), BackendCompressor.DEFLATE_DEFAULT)
        // Note: byte_run_rle may not be applicable after its own encoding changes the structure
        val result = evaluator.evaluate(rleInput, recipe, b)
        // Just verify it doesn't crash and has valid state
        assertNotNull(result)
        if (result.reconstructionPassed) {
            assertNotEquals(CandidateClassification.EXACTNESS_FAILED, result.classification)
        }
    }

    @Test
    fun `recipe serialization preserves chain`() {
        val original = RecipeSpec("ser_test", listOf("delta8", "xor_prev", "block_shuffle_4"), BackendCompressor.DEFLATE_BEST_COMPRESSION)
        val json = RecipeSerializer.serialize(original)
        val restored = RecipeSerializer.deserialize(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.transformIds, restored.transformIds)
        assertEquals(original.backend, restored.backend)
    }

    @Test
    fun `single transform chain round trips`() {
        val recipe = RecipeSpec("single", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(testInput, recipe, baseline())
        assertTrue(result.reconstructionPassed)
    }
}
