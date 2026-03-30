package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class XorPrevByteTransformTest {
    private val transform = XorPrevByteTransform()

    @Test
    fun `round trip on arbitrary bytes`() {
        val input = ByteArray(200) { (it * 13 % 256).toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on all same bytes`() {
        val input = ByteArray(100) { 0xAB.toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable for size 2`() {
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(2)))
    }

    @Test
    fun `not applicable for size 1`() {
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(ByteArray(1)))
    }

    @Test
    fun `metadata is empty`() {
        val output = transform.encode(ByteArray(10))
        assertEquals(0, output.metadata.size)
    }

    @Test
    fun `constant input produces zeros after first byte`() {
        val input = ByteArray(50) { 0x55 }
        val output = transform.encode(input)
        assertEquals(0x55.toByte(), output.bytes[0])
        for (i in 1 until output.bytes.size) {
            assertEquals(0x00.toByte(), output.bytes[i])
        }
    }
}
