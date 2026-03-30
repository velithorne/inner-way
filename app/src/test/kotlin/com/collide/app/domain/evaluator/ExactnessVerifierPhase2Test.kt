package com.collide.app.domain.evaluator

import com.collide.app.domain.model.VerificationMethod
import org.junit.Assert.*
import org.junit.Test

class ExactnessVerifierPhase2Test {

    @Test
    fun `verifyFull passes for identical arrays`() {
        val data = ByteArray(200) { (it % 64).toByte() }
        val result = ExactnessVerifier.verifyFull(data, data.copyOf())
        assertTrue(result.passed)
        assertEquals(VerificationMethod.BYTE_EQUALITY_AND_SHA256, result.method)
        assertNull(result.failureReason)
    }

    @Test
    fun `verifyFull hashes match for identical arrays`() {
        val data = "Hello COLLIDE Phase 2".toByteArray()
        val result = ExactnessVerifier.verifyFull(data, data.copyOf())
        assertTrue(result.hashesMatch)
        assertEquals(result.originalSha256, result.reconstructedSha256)
    }

    @Test
    fun `verifyFull fails for different content`() {
        val a = ByteArray(100) { 0x00 }
        val b = ByteArray(100) { 0x01 }
        val result = ExactnessVerifier.verifyFull(a, b)
        assertFalse(result.passed)
        assertFalse(result.hashesMatch)
        assertNotEquals(result.originalSha256, result.reconstructedSha256)
    }

    @Test
    fun `verifyFull fails for different lengths`() {
        val a = ByteArray(100)
        val b = ByteArray(99)
        val result = ExactnessVerifier.verifyFull(a, b)
        assertFalse(result.passed)
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("Length mismatch"))
    }

    @Test
    fun `verifyFull fails for single byte flip`() {
        val a = ByteArray(100)
        val b = ByteArray(100)
        b[50] = 0xFF.toByte()
        val result = ExactnessVerifier.verifyFull(a, b)
        assertFalse(result.passed)
    }

    @Test
    fun `sha256Hex produces 64 hex chars`() {
        val hash = ExactnessVerifier.sha256Hex("test".toByteArray())
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `sha256Hex is deterministic`() {
        val data = ByteArray(500) { it.toByte() }
        assertEquals(ExactnessVerifier.sha256Hex(data), ExactnessVerifier.sha256Hex(data))
    }

    @Test
    fun `sha256Hex differs for different data`() {
        val a = ExactnessVerifier.sha256Hex(ByteArray(10) { 0x00 })
        val b = ExactnessVerifier.sha256Hex(ByteArray(10) { 0x01 })
        assertNotEquals(a, b)
    }

    @Test
    fun `verifyFull empty arrays pass`() {
        val result = ExactnessVerifier.verifyFull(ByteArray(0), ByteArray(0))
        assertTrue(result.passed)
        assertTrue(result.hashesMatch)
    }
}
