package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class ByteRunRLETransformTest {
    private val transform = ByteRunRLETransform()

    @Test
    fun `round trip on repetitive data`() {
        val input = ByteArray(100) { 0x42 }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on mixed data`() {
        val input = byteArrayOf(0x01, 0x01, 0x01, 0x01, 0x02, 0x03, 0x04, 0x05, 0x05, 0x05, 0x05)
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on random bytes`() {
        val input = ByteArray(256) { it.toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable when run of 3 exists`() {
        val input = byteArrayOf(0x01, 0x02, 0x03, 0x03, 0x03)
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(input))
    }

    @Test
    fun `not applicable for short input`() {
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(ByteArray(2)))
    }

    @Test
    fun `not applicable when no runs`() {
        val input = ByteArray(10) { it.toByte() }
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(input))
    }

    @Test
    fun `encodes single large run compactly`() {
        val input = ByteArray(1000) { 0x00 }
        val output = transform.encode(input)
        assertTrue("RLE output should be much smaller than input", output.bytes.size < input.size / 2)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `metadata contains original size`() {
        val input = ByteArray(1234) { 0xAB.toByte() }
        val output = transform.encode(input)
        assertEquals(4, output.metadata.size)
        val originalSize = java.nio.ByteBuffer.wrap(output.metadata).int
        assertEquals(1234, originalSize)
    }
}
