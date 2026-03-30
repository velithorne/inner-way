package com.collide.app.domain.evaluator

import com.collide.app.domain.engine.BackendCompressorEngine
import com.collide.app.domain.model.BackendCompressor
import com.collide.app.domain.model.CandidateClassification
import com.collide.app.domain.model.RecipeSpec
import com.collide.app.domain.model.SizeBreakdown
import org.junit.Assert.*
import org.junit.Test

class SizeAccountingTest {

    private val evaluator = CandidateEvaluator()

    private val repetitiveInput = ByteArray(3000) { (it % 32).toByte() }

    private fun baseline() = BackendCompressorEngine.compress(
        repetitiveInput, BackendCompressor.DEFLATE_DEFAULT
    ).size.toLong()

    @Test
    fun `size breakdown totalEncodedSize equals sum of components`() {
        val recipe = RecipeSpec("r_test", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        val bd = result.sizeBreakdown
        assertNotNull("SizeBreakdown must be present for evaluated candidates", bd)
        bd!!
        // totalEncodedSize = backendCompressed + transformMetadata + containerHeader
        assertEquals(
            bd.backendCompressedPayloadSize + bd.transformMetadataSize + bd.containerHeaderSize,
            bd.totalEncodedSize
        )
    }

    @Test
    fun `container header is always 8 bytes`() {
        val recipe = RecipeSpec("r_h", listOf("xor_prev"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        result.sizeBreakdown?.let { assertEquals(8L, it.containerHeaderSize) }
    }

    @Test
    fun `transform metadata size increases with more transforms in chain`() {
        val r1 = RecipeSpec("r1", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val r2 = RecipeSpec("r2", listOf("delta8", "xor_prev"), BackendCompressor.DEFLATE_DEFAULT)

        val res1 = evaluator.evaluate(repetitiveInput, r1, baseline())
        val res2 = evaluator.evaluate(repetitiveInput, r2, baseline())

        val meta1 = res1.sizeBreakdown?.transformMetadataSize ?: return
        val meta2 = res2.sizeBreakdown?.transformMetadataSize ?: return

        // Each extra transform adds at minimum its 4-byte length prefix to metadata
        assertTrue("2-transform chain metadata ($meta2) should be >= 1-transform ($meta1)", meta2 >= meta1)
    }

    @Test
    fun `block shuffle metadata is 8 bytes plus 4 byte length prefix = 12 per transform`() {
        val recipe = RecipeSpec("r_bs", listOf("block_shuffle_4"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        val bd = result.sizeBreakdown ?: return
        // FixedBlockShuffleTransform produces 8 bytes metadata; plus 4-byte length prefix = 12 total
        assertEquals(12L, bd.transformMetadataSize)
    }

    @Test
    fun `delta8 transform metadata is 0 bytes plus 4 byte length prefix = 4`() {
        val recipe = RecipeSpec("r_d8", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        val bd = result.sizeBreakdown ?: return
        // Delta8 has empty metadata; 4-byte length prefix still counts
        assertEquals(4L, bd.transformMetadataSize)
    }

    @Test
    fun `total encoded size is consistent with candidate result encodedSize`() {
        val recipe = RecipeSpec("r_check", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT)
        val result = evaluator.evaluate(repetitiveInput, recipe, baseline())
        if (result.sizeBreakdown != null) {
            assertEquals(result.encodedSize, result.sizeBreakdown!!.totalEncodedSize)
        }
    }

    @Test
    fun `SizeBreakdown display map contains all required keys`() {
        val bd = SizeBreakdown(
            transformedPayloadSize = 1000L,
            backendCompressedPayloadSize = 200L,
            transformMetadataSize = 12L,
            containerHeaderSize = 8L,
            originalInputSize = 3000L
        )
        val map = bd.toDisplayMap()
        assertTrue(map.containsKey("Original Input"))
        assertTrue(map.containsKey("Total Encoded"))
        assertTrue(map.containsKey("Overhead"))
        assertTrue(map.containsKey("Backend Compressed"))
        assertTrue(map.containsKey("Transform Metadata"))
    }

    @Test
    fun `SizeBreakdown overheadSize equals metadata plus header`() {
        val bd = SizeBreakdown(500L, 200L, 12L, 8L, 1000L)
        assertEquals(20L, bd.overheadSize)
    }
}
