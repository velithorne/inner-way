package com.collide.app.domain.transforms

import org.junit.Assert.*
import org.junit.Test

class IdentityTransformTest {
    private val transform = IdentityTransform()

    @Test
    fun `round trip returns original bytes`() {
        val input = "Hello, World!".toByteArray()
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `always applicable`() {
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(0)))
        assertEquals(ApplicabilityStatus.APPLICABLE, transform.checkApplicability(ByteArray(1000)))
    }

    @Test
    fun `metadata is empty`() {
        val output = transform.encode("test".toByteArray())
        assertEquals(0, output.metadata.size)
    }

    @Test
    fun `empty input round trips`() {
        val input = ByteArray(0)
        val output = transform.encode(input)
        val decoded = transform.decode(output.bytes, output.metadata)
        assertArrayEquals(input, decoded)
    }
}
