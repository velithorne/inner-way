package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class ZeroRunRLETransformTest {
    private val transform = ZeroRunRLETransform()

    @Test
    fun `round trip on data with zeros`() {
        val input = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x02, 0x03, 0x00)
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on all zeros`() {
        val input = ByteArray(500)
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable when zero present`() {
        val input = byteArrayOf(0x01, 0x00, 0x02)
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(input))
    }

    @Test
    fun `not applicable when no zeros`() {
        val input = ByteArray(10) { (it + 1).toByte() }
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(input))
    }

    @Test
    fun `large zero run compresses well`() {
        val input = ByteArray(1000)
        val output = transform.encode(input)
        assertTrue(output.bytes.size < 10)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `metadata has original size`() {
        val input = ByteArray(789)
        val output = transform.encode(input)
        val size = java.nio.ByteBuffer.wrap(output.metadata).int
        assertEquals(789, size)
    }
}
