package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class MoveToFrontTransformTest {
    private val transform = MoveToFrontTransform()

    @Test
    fun `round trip on simple input`() {
        val input = "AABBC".toByteArray()
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on arbitrary bytes`() {
        val input = ByteArray(256) { it.toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `round trip on repeated bytes`() {
        val input = ByteArray(100) { 0xAB.toByte() }
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `applicable for non-empty input`() {
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(1)))
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(1000)))
    }

    @Test
    fun `not applicable for empty input`() {
        assertEquals(ApplicabilityStatus.NOT_APPLICABLE, transform.checkApplicability(ByteArray(0)))
    }

    @Test
    fun `metadata is empty`() {
        val output = transform.encode("test".toByteArray())
        assertEquals(0, output.metadata.size)
    }

    @Test
    fun `repeated symbol encodes to zero after first occurrence`() {
        val input = ByteArray(10) { 0x41 } // all 'A'
        val output = transform.encode(input)
        // First 'A' maps to rank 65 (its initial position), subsequent ones map to 0
        assertEquals(0x00.toByte(), output.bytes[1])
        assertEquals(0x00.toByte(), output.bytes[2])
    }
}
