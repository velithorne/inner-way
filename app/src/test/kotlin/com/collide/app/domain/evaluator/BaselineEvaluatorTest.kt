package com.collide.app.domain.evaluator

import com.collide.app.domain.model.BaselineStrategy
import org.junit.Assert.*
import org.junit.Test

class BaselineEvaluatorTest {
    private val evaluator = BaselineEvaluator()

    @Test
    fun `raw deflate baseline is deterministic`() {
        val input = ByteArray(1000) { (it % 64).toByte() }
        val r1 = evaluator.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        val r2 = evaluator.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        assertEquals(r1.encodedSize, r2.encodedSize)
        assertEquals(r1.strategy, r2.strategy)
    }

    @Test
    fun `best compression baseline is deterministic`() {
        val input = ByteArray(1000) { (it % 128).toByte() }
        val r1 = evaluator.evaluate(input, BaselineStrategy.DEFLATE_BEST_COMPRESSION)
        val r2 = evaluator.evaluate(input, BaselineStrategy.DEFLATE_BEST_COMPRESSION)
        assertEquals(r1.encodedSize, r2.encodedSize)
    }

    @Test
    fun `baseline size is positive`() {
        val input = ByteArray(500) { 0x42 }
        val result = evaluator.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        assertTrue(result.encodedSize > 0)
    }

    @Test
    fun `different strategies can produce different sizes`() {
        val input = ByteArray(5000) { (it % 32).toByte() }
        val r1 = evaluator.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        val r2 = evaluator.evaluate(input, BaselineStrategy.DEFLATE_BEST_COMPRESSION)
        // Best compression should be <= default (may equal on simple data)
        assertTrue(r2.encodedSize <= r1.encodedSize)
    }

    @Test
    fun `strategy is stored in result`() {
        val input = ByteArray(100)
        val result = evaluator.evaluate(input, BaselineStrategy.RAW_DEFLATE)
        assertEquals(BaselineStrategy.RAW_DEFLATE, result.strategy)
    }
}
