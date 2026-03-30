package com.collide.app.domain.evaluator

import com.collide.app.domain.detector.EventRecorder
import com.collide.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class EventRecorderPhase2Test {

    private val recorder = EventRecorder()

    private val baseline = BaselineResult(
        strategy = BaselineStrategy.RAW_DEFLATE,
        encodedSize = 1000L,
        elapsedMs = 5L
    )
    private val input = InputSample("test.bin", null, ByteArray(5000))

    private fun makeVerification(passed: Boolean, hashesMatch: Boolean = passed) =
        VerificationResult(
            passed = passed,
            method = VerificationMethod.BYTE_EQUALITY_AND_SHA256,
            originalSha256 = "abc123",
            reconstructedSha256 = if (hashesMatch) "abc123" else "different",
            failureReason = if (!passed) "Test failure" else null
        )

    @Test
    fun `creates event when all Phase 2 criteria met`() {
        val result = CandidateResult(
            recipeSpec = RecipeSpec("r1", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            classification = CandidateClassification.STRICT_WINNER,
            encodedSize = 800L, baselineSize = 1000L, byteSavings = 200L,
            reconstructionPassed = true, elapsedMs = 10L,
            verificationResult = makeVerification(true),
            sizeBreakdown = SizeBreakdown(900L, 790L, 2L, 8L, 5000L),
            candidateIndex = 5
        )
        val event = recorder.maybeCreateEvent(result, input, baseline)
        assertNotNull(event)
        assertEquals("abc123", event!!.originalSha256)
        assertEquals("abc123", event.reconstructedSha256)
        assertEquals(VerificationMethod.BYTE_EQUALITY_AND_SHA256.name, event.verificationMethod)
        assertEquals(5, event.candidateIndex)
        assertEquals(RunConfigSnapshot.ENGINE_VERSION, event.engineVersion)
        assertEquals(790L, event.backendCompressedSize)
    }

    @Test
    fun `rejects when verification result not passed`() {
        val result = CandidateResult(
            recipeSpec = RecipeSpec("r2", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            classification = CandidateClassification.STRICT_WINNER,
            encodedSize = 800L, baselineSize = 1000L, byteSavings = 200L,
            reconstructionPassed = true, elapsedMs = 10L,
            verificationResult = makeVerification(false)
        )
        assertNull(recorder.maybeCreateEvent(result, input, baseline))
    }

    @Test
    fun `rejects when hashes do not match`() {
        val result = CandidateResult(
            recipeSpec = RecipeSpec("r3", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            classification = CandidateClassification.STRICT_WINNER,
            encodedSize = 800L, baselineSize = 1000L, byteSavings = 200L,
            reconstructionPassed = true, elapsedMs = 10L,
            verificationResult = makeVerification(passed = true, hashesMatch = false)
        )
        assertNull(recorder.maybeCreateEvent(result, input, baseline))
    }

    @Test
    fun `rejects hash_mismatch classification`() {
        val result = CandidateResult(
            recipeSpec = RecipeSpec("r4", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            classification = CandidateClassification.HASH_MISMATCH,
            encodedSize = 800L, baselineSize = 1000L, byteSavings = 200L,
            reconstructionPassed = false, elapsedMs = 10L
        )
        assertNull(recorder.maybeCreateEvent(result, input, baseline))
    }

    @Test
    fun `run config json is stored when provided`() {
        val config = RunConfigSnapshot(
            runMode = "BALANCED", baselineStrategy = "RAW_DEFLATE",
            maxCandidates = 80, maxChainLength = 3, enabledTransformIds = emptyList()
        )
        val result = CandidateResult(
            recipeSpec = RecipeSpec("r5", listOf("delta8"), BackendCompressor.DEFLATE_DEFAULT),
            classification = CandidateClassification.STRICT_WINNER,
            encodedSize = 800L, baselineSize = 1000L, byteSavings = 200L,
            reconstructionPassed = true, elapsedMs = 10L,
            verificationResult = makeVerification(true)
        )
        val event = recorder.maybeCreateEvent(result, input, baseline, config)
        assertNotNull(event)
        assertTrue("runConfigJson should not be empty", event!!.runConfigJson.isNotEmpty())
        assertTrue(event.runConfigJson.contains("BALANCED"))
    }
}
