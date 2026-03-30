package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class FixedBlockShuffleTransformTest {
    private val transform4 = FixedBlockShuffleTransform(blockSize = 4)
    private val transform8 = FixedBlockShuffleTransform(blockSize = 8)

    @Test
    fun `round trip block size 4`() {
        val input = ByteArray(64) { it.toByte() }
        val output = transform4.encode(input)
        val decoded = transform4.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip block size 8`() {
        val input = ByteArray(128) { (it * 3 % 256).toByte() }
        val output = transform8.encode(input)
        val decoded = transform8.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip with trailing bytes`() {
        // 65 bytes = 16 full blocks of 4 + 1 trailing byte
        val input = ByteArray(65) { it.toByte() }
        val output = transform4.encode(input)
        val decoded = transform4.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on all zeros`() {
        val input = ByteArray(100)
        val output = transform4.encode(input)
        val decoded = transform4.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable for size 8 with block 4`() {
        assertEquals(ApplicabilityStatus.APPLICABLE, transform4.checkApplicability(ByteArray(8)))
    }

    @Test
    fun `not applicable for small input`() {
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform4.checkApplicability(ByteArray(7)))
    }

    @Test
    fun `metadata contains block size and original length`() {
        val input = ByteArray(100)
        val output = transform4.encode(input)
        assertEquals(8, output.metadata.size) // 4 bytes block size + 4 bytes original size
        val buf = java.nio.ByteBuffer.wrap(output.metadata)
        val blockSize = buf.int
        val origSize = buf.int
        assertEquals(4, blockSize)
        assertEquals(100, origSize)
    }

    @Test
    fun `shuffled output differs from input for non-trivial data`() {
        val input = ByteArray(16) { it.toByte() }
        val output = transform4.encode(input)
        assertFalse(input.contentEquals(output.bytes))
    }
}
