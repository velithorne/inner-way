package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class Delta8TransformTest {
    private val transform = Delta8Transform()

    @Test
    fun `round trip on incrementing bytes`() {
        val input = ByteArray(256) { it.toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on random bytes`() {
        val input = ByteArray(500) { (it * 37 % 256).toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable for size 2 or more`() {
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(2)))
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(1000)))
    }

    @Test
    fun `not applicable for size 1`() {
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(ByteArray(1)))
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(ByteArray(0)))
    }

    @Test
    fun `metadata is empty`() {
        val output = transform.encode(ByteArray(10) { it.toByte() })
        assertEquals(0, output.metadata.size)
    }

    @Test
    fun `constant sequence encodes to zeros after first byte`() {
        val input = ByteArray(100) { 0x42 }
        val output = transform.encode(input)
        assertEquals(0x42.toByte(), output.bytes[0])
        for (i in 1 until output.bytes.size) {
            assertEquals(0x00.toByte(), output.bytes[i])
        }
    }

    @Test
    fun `wrap-around arithmetic is correct`() {
        val input = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0xFF.toByte())
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }
}
