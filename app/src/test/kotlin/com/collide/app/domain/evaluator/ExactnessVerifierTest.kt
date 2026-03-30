package com.collide.app.domain.evaluator

import org.junit.Assert.*
import org.junit.Test

class ExactnessVerifierTest {

    @Test
    fun `identical arrays pass`() {
        val a = ByteArray(100) { it.toByte() }
        val b = ByteArray(100) { it.toByte() }
        assertTrue(ExactnessVerifier.verify(a, b))
    }

    @Test
    fun `different content fails`() {
        val a = ByteArray(10) { it.toByte() }
        val b = ByteArray(10) { (it + 1).toByte() }
        assertFalse(ExactnessVerifier.verify(a, b))
    }

    @Test
    fun `different lengths fail`() {
        val a = ByteArray(10)
        val b = ByteArray(11)
        assertFalse(ExactnessVerifier.verify(a, b))
    }

    @Test
    fun `empty arrays pass`() {
        assertTrue(ExactnessVerifier.verify(ByteArray(0), ByteArray(0)))
    }

    @Test
    fun `single byte difference fails`() {
        val a = ByteArray(100) { 0x00 }
        val b = ByteArray(100) { 0x00 }
        b[50] = 0x01
        assertFalse(ExactnessVerifier.verify(a, b))
    }
}
